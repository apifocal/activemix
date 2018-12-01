/*
 * Copyright (c) 2017-2018 apifocal LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apifocal.activemx.tools.token;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apifocal.activemx.jaas.token.Settings;
import org.apifocal.activemx.jaas.token.TokenValidationException;
import org.apifocal.activemx.jaas.token.Tokens;
import org.apifocal.activemx.jaas.token.verifiers.TokenSignerValidator;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableSet;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

/**
 * JMS Token Generation Utility for Apache ActiveMQ
 */
public class TokenApp {

    private static final String ACTION_HELP = "help";
    private static final String ACTION_CREATE = "create";
    private static final String ACTION_SHOW = "show";
    private static final String[] HELP_USAGE = {"\n",
        "  amix-token create [options] <signing-key>\n",
        "  amix-token show [options] [<token>]"};
    private static final String CLAIM_ACL = "acl";
    private static final String DEFAULT_ACL = "rw";
    private static final String DEFAULT_EXPIRES = "90d";
    private static final String NEVER_EXPIRES = "never";

    private static final Option OPTION_HELP = Option
        .builder("h").longOpt("help").build();
    private static final Option OPTION_USER = Option
        .builder("u").longOpt("user").desc("Username for creating JMS connection.").hasArg(true).argName("user").build();
    private static final Option OPTION_ISSUER = Option
        .builder("i").longOpt("issuer").desc("Issuer of authorization token (key owner)").hasArg(true).argName("issuer").build();
    private static final Option OPTION_APP = Option
        .builder("a").longOpt("app").desc("JMS application id (optional)").hasArg(true).argName("app").build();
    private static final Option OPTION_ACL = Option
        .builder().longOpt("acl").desc("Token access privileges").hasArg(true).argName("acl").build();
    private static final Option OPTION_EXPIRATION = Option
        .builder().longOpt("exp").desc("Token expiration time").hasArg(true).argName("exp").build();
    private static final Option OPTION_VERIFY = Option
        .builder().longOpt("verify").desc("Token signature verification").hasArg(false).build();
    private static final Option OPTION_KEYS = Option
        .builder().longOpt("keys").desc("Authorized keys file").hasArg(true).argName("keys").build();
    private static final Option OPTION_RAW = Option
        .builder().longOpt("raw").desc("Authorized keys file").hasArg(false).build();

    private static final Option[] OPTIONS = {
            OPTION_HELP,
            OPTION_USER,
            OPTION_ISSUER,
            OPTION_APP,
            OPTION_ACL,
            OPTION_EXPIRATION,
            OPTION_VERIFY,
            OPTION_KEYS,
            OPTION_RAW,
    };
    private static final Option[] OPTIONS_CREATE = {
            OPTION_HELP,
            OPTION_USER,
            OPTION_ISSUER,
            OPTION_APP,
            OPTION_ACL,
            OPTION_EXPIRATION,
    };
    private static final Option[] OPTIONS_SHOW = {
            OPTION_HELP,
            OPTION_VERIFY,
            OPTION_KEYS,
            OPTION_RAW,
    };

    private static final String OPTIONS_FOOTER = "\n"
        + "Generates JWT token as password for ActiveMQ brokers\n"
        + "For more information visit https://docs.silkmq.com";

    private static final Map<String, String> CLAIM_NAMES = new HashMap<>();
    private static final String[][] CLAIM_NAME_LIST = {
        {"sub", "User"},
        {"iss", "Issuer"},
        {"aud", "App"},
        {"iat", "Issued-At"},
        {"exp", "Expires"},
        {"nbf", "Not-Before"},
    };

    static {
        Arrays.asList(CLAIM_NAME_LIST).forEach(a -> {
            CLAIM_NAMES.put(a[0], a[1]);
        });
    };

    public static void main(String[] args) {
        String action = "";
        CommandLine cli = null;

        try {
            if (args.length > 0) {
                action = args[0].toLowerCase();
                action = action.startsWith("-") ? action.replaceAll("-", "") : action;
                action = action.equals(OPTION_HELP.getOpt()) ? ACTION_HELP : action;
                cli = parse(Arrays.copyOfRange(args, 1, args.length));
            }

            boolean validArgs = !ImmutableSet.of(ACTION_HELP, ACTION_CREATE, ACTION_SHOW).contains(action) ? false
                : ACTION_CREATE.equals(action) ? validateCreateArgs(cli)
                : ACTION_SHOW.equals(action) ? validateShowArgs(cli)
                : cli.hasOption(OPTION_HELP.getOpt());
            if (!validArgs) {
                throw new ParseException("Missing required arguments");
            }
        } catch (ParseException e) {
            usage(action, e.getLocalizedMessage());
            return;
        }

        if (cli.hasOption("h")) {
            usage(action, null);
            return;
        }

        try {
            if (ACTION_CREATE.equals(action)) {
                String token = createToken(cli);
                System.out.println(token != null ? token : "ERROR");
            } else if (ACTION_SHOW.equals(action)) {
                showToken(cli);
            }
        } catch (Exception e) {
            System.out.println(e.getLocalizedMessage());
            helpCreate(System.out);
        }
    }

    private static boolean validateCreateArgs(CommandLine cli) {
        // TODO: maybe log some verbose warnings
        return cli.hasOption("h") || (cli.hasOption("u") && cli.hasOption("i") && cli.getArgs().length == 1);
    }

    private static boolean validateShowArgs(CommandLine cli) {
        return cli.hasOption("h") || (cli.getArgs().length == 1 && (!cli.hasOption("verify") || cli.hasOption("keys")));
    }

    public static String createToken(final CommandLine cli) throws Exception {
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder();
        Tokens.subject(claims, Objects.requireNonNull(cli.getOptionValue("u"), "Missing option 'user'"));
        Tokens.issuer(claims, Objects.requireNonNull(cli.getOptionValue("i"), "Missing option 'issuer'"));

        String[] audience = cli.getOptionValues("a");
        if (audience != null) {
            if (audience.length == 1) {
                Tokens.audience(claims, audience[0]);
            } else if (audience.length > 1) {
                Tokens.audience(claims, Arrays.asList(audience));
            }            
        }
        String acl = cli.getOptionValue("acl");
        Tokens.claim(claims, CLAIM_ACL, acl != null ? acl : DEFAULT_ACL);

        String key =  cli.getArgs()[0]; // if argument validation was done well, this could not be null
        Path sk = Paths.get(key);
        if (!Files.exists(sk) || !Files.isRegularFile(sk)) {
            throw new IllegalArgumentException("Invalid signing key. Check command line arguments.");
        }

        // TODO: make it optional?
        Date now = new Date();
        Tokens.issueTime(claims, now);
        long exp = lifespan(cli.getOptionValue("exp"));
        if (exp > 0) {
            Tokens.expiration(claims, new Date(now.getTime() + exp));
        }

        return Tokens.createToken(claims.build(), new String(Files.readAllBytes(sk), Charsets.UTF_8), new StdinPasswordProvider(key));
    }

    public static void showToken(final CommandLine cli) {
        SignedJWT parsedToken;
        Map<String, Object> claims;
        String json;
        try {
            parsedToken = SignedJWT.parse(cli.getArgs()[0]);
            claims = new HashMap<>(parsedToken.getJWTClaimsSet().getClaims());
            json = cli.hasOption("raw") ? parsedToken.getJWTClaimsSet().toJSONObject().toString() : null;
        } catch (java.text.ParseException e) {
            System.out.println("Not a valid signed token.");
            return;
        }

        StringBuilder builder = new StringBuilder();
        if (cli.hasOption("verify")) {
            builder.append("Signature verification: ");

            Map<String, String> settings = new HashMap<>();
            settings.put("keys", Objects.requireNonNull(cli.getOptionValue("keys")));
            TokenSignerValidator validator = new TokenSignerValidator(new Settings(settings));
            try {
                validator.validate(parsedToken, null);
                builder.append("PASSED.");
            } catch (TokenValidationException e) {
                builder.append("FAILED:\n").append("  ").append(e.getLocalizedMessage());
            }
            builder.append('\n').append('\n');
        }
        if (json == null) {
            builder.append("Claims:\n");
            printClaims(claims, builder);
        } else {
            builder.append(json).append("\n");
        }
        System.out.println(builder.toString());
    }

    private static void printClaims(Map<String, Object> claims, final StringBuilder builder) {
        printKnownClaim(claims, builder, "sub");
        printKnownClaim(claims, builder, "iss");
        printKnownClaim(claims, builder, "aud");
        printKnownClaim(claims, builder, "iat");
        printKnownClaim(claims, builder, "exp");
        printKnownClaim(claims, builder, "nbf");
        claims.forEach((k, v) -> {
            printClaim(builder, k, v.toString());
        });
    }

    private static void printKnownClaim(Map<String, Object> claims, final StringBuilder builder, String name) {
        String claimName = CLAIM_NAMES.get(name);
        Object value = claims.remove(name);
        if (value != null) {
            printClaim(builder, claimName == null ? name : claimName, value);
        }
    }

    private static void printClaim(final StringBuilder builder, String name, Object value) {
        String claimName = CLAIM_NAMES.get(name);
        claimName = claimName == null ? name : claimName;

        // value cannot be null in this context
        String text = value.toString();
        if (value instanceof Date) {
            text = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format((Date)value);
        }
        builder.append("  ").append(claimName).append(": ").append(text).append("\n");
    }

    private static long lifespan(String exp) {
        String expires = exp != null ? exp : DEFAULT_EXPIRES;
        if (NEVER_EXPIRES.compareTo(expires.toLowerCase()) == 0) {
            return -1L;
        }
        long delta = 0;
        try {
            char last = expires.charAt(expires.length() - 1);
            if (Character.isLetter(last)) {
                int num = Integer.parseInt(expires.substring(0, expires.length() - 1));
                switch (last) {
                case 'Z': break; // TODO: add support for explicitly defined time
                case 'd': delta = Tokens.days(num); break;
                case 'h': delta = Tokens.hours(num); break;
                case 'm': delta = Tokens.minutes(num); break;
                default: break; // TODO: report unsupported qualifier; use default
                }
            } else {
                return Tokens.seconds(Integer.parseInt(expires));
            }
        } catch (Exception e) {
            // TODO: report
        }
        return delta;
    }

    public static CommandLine parse(String[] args) throws ParseException {
        CommandLineParser parser = new DefaultParser();
        return parser.parse(fromArray(OPTIONS), args);
    }

    public static void usage(String action) {
        usage(action, null);
    }

    public static void usage(String action, String message) {
        if (message != null && !message.isEmpty()) {
            System.out.println(message);
        }
        if (ACTION_CREATE.equals(action)) {
            helpCreate(System.out);
        } else if (ACTION_SHOW.equals(action)) {
            helpShow(System.out);
        } else {
            help(System.out);
        }
    }

    private static void help(PrintStream out) {
        out.println();

        PrintWriter pw = new PrintWriter(out);
        HelpFormatter formatter = new HelpFormatter();
        formatter.setOptionComparator(new CustomComparator());
        formatter.printHelp(pw, HelpFormatter.DEFAULT_WIDTH, HELP_USAGE[0] + HELP_USAGE[1] + HELP_USAGE[2], "\nOptions:", filterOptions(fromArray(OPTIONS)),
             HelpFormatter.DEFAULT_LEFT_PAD, HelpFormatter.DEFAULT_DESC_PAD, OPTIONS_FOOTER, false);
        pw.flush();
    }

    private static void helpCreate(PrintStream out) {
        out.println();

        PrintWriter pw = new PrintWriter(out);
        HelpFormatter formatter = new HelpFormatter();
        formatter.setOptionComparator(new CustomComparator());
        formatter.printHelp(pw, HelpFormatter.DEFAULT_WIDTH, HELP_USAGE[0] + HELP_USAGE[1], "\nOptions:", filterOptions(fromArray(OPTIONS_CREATE)),
             HelpFormatter.DEFAULT_LEFT_PAD, HelpFormatter.DEFAULT_DESC_PAD, OPTIONS_FOOTER, false);
        pw.flush();
    }

    private static void helpShow(PrintStream out) {
        out.println();

        PrintWriter pw = new PrintWriter(out);
        HelpFormatter formatter = new HelpFormatter();
        formatter.setOptionComparator(new CustomComparator());
        formatter.printHelp(pw, HelpFormatter.DEFAULT_WIDTH, HELP_USAGE[0] + HELP_USAGE[2], "\nOptions:", filterOptions(fromArray(OPTIONS_SHOW)),
             HelpFormatter.DEFAULT_LEFT_PAD, HelpFormatter.DEFAULT_DESC_PAD, OPTIONS_FOOTER, false);
        pw.flush();
    }

    private static Options filterOptions(Options options) {
        Options result = new Options();
        options.getOptions().forEach(o -> {
            String desc = o.getDescription();
            if (desc == null || !o.getDescription().startsWith("[NO HELP]")) {
                result.addOption(o);
            }
        });
        return result;
    }

    private static Options fromArray(final Option[] options) {
        final Options result = new Options();
        Arrays.asList(options).forEach(o -> result.addOption(o));
        return result;
    }

    private static class CustomComparator implements Comparator<Option>, Serializable {
        private static final long serialVersionUID = 1L;

        public int compare(Option o1, Option o2) {
            return index(o1.getId()) - index(o2.getId());
        }

        private int index(int opt) {
            // not very pretty, but fast;
            // ...not that it matters much if we reached this point
            int result = 0;
            switch (opt) {
                case 'h': result = 1; break;
                case 'u': result = 2; break;
                case 'i': result = 3; break;
                case 'a': result = 4; break;
                default: result = 16;
            }
            return result;
        }
    }
}
