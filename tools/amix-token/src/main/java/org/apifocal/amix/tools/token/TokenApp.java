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
package org.apifocal.amix.tools.token;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apifocal.amix.jaas.token.Tokens;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import net.minidev.json.JSONObject;

/**
 * JMS Token Generation Utility for Apache ActiveMQ
 */
public class TokenApp {

    private static final Option OPTION_HELP = Option
        .builder("h").longOpt("help").build();
    private static final Option OPTION_USER = Option
        .builder("u").longOpt("user").desc("User for JMS connection").hasArg(true).argName("user").build();
    private static final Option OPTION_APP = Option
        .builder("a").longOpt("app").desc("Messaging application").hasArg(true).argName("app").build();
    private static final Option OPTION_ISSUER = Option
            .builder("i").longOpt("issuer").desc("Issuer account").hasArg(true).argName("issuer").build();
    private static final Option OPTION_KEY = Option
            .builder("k").longOpt("sign-key").desc("Signing key").hasArg(true).argName("key").build();

    private static final Options OPTIONS = new Options();
    private static final Option[] OPTIONS_LIST = {
            OPTION_HELP,
            OPTION_USER,
            OPTION_APP,
            OPTION_ISSUER,
            OPTION_KEY,
    };
    private static final String OPTIONS_FOOTER = "\n"
    	+ "Generates JWT token as password for ActiveMQ brokers\n"
        + "For more information visit https://docs.silkmq.com";
    static {
        Arrays.asList(OPTIONS_LIST).forEach(o -> {
            OPTIONS.addOption(o);
        });
    };

    public static void main(String[] args) {
        CommandLine cli = null;
        try {
            cli = parse(args);
            boolean missingArgs = !cli.hasOption("u") || !cli.hasOption("i") || !cli.hasOption("k");
            if (missingArgs && !cli.hasOption("h")) {
                throw new ParseException("Missing required arguments");
            }
        } catch (ParseException e) {
            usage(e.getLocalizedMessage());
            return;
        }

        if (cli.hasOption("h")) {
            help(System.out);
            return;
        }

        String token = createToken(cli);
        System.out.println(token != null ? token : "ERROR");
    }

    public static String createToken(final CommandLine cli) {
    	String user = cli.getOptionValue("u");
    	String issuer = cli.getOptionValue("i");
    	String app =  cli.getOptionValue("a");
    	String key =  cli.getOptionValue("k");

    	Path sk = Paths.get(key);
    	if (!Files.exists(sk) || !Files.isRegularFile(sk)) {
    		// TODO: throw an exception
    		return null;
    	}

    	JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
            .subject(user)
            .issuer("urn:" + issuer)
            .issueTime(new Date());
    	claims = app != null ? claims.audience(app) : claims;

    	// now let's sign it...
    	SignedJWT signedJWT = null;
    	try {
			String privkey = new String(Files.readAllBytes(sk), Charsets.UTF_8);
			KeyPair kp = Tokens.readKeyPair(privkey);
			if (!"RSA".equals(kp.getPublic().getAlgorithm())) {
				// TODO: LOG, complain...
				return null;
			}
			RSAPrivateKey privateKey = kp.getPrivate() instanceof RSAPrivateKey ? (RSAPrivateKey)kp.getPrivate() : null;
	        JWSSigner signer = new RSASSASigner(privateKey);
	        signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims.build());
		} catch (Exception e) {
			System.out.print(e.getLocalizedMessage());
			return null;
		}

    	return signedJWT.serialize();
    }

    public static CommandLine parse(String[] args) throws ParseException {
        CommandLineParser parser = new DefaultParser();
        return parser.parse(OPTIONS, args);
    }

    public static void usage() {
        usage(null);
    }

    public static void usage(String message) {
        if (message != null && !message.isEmpty()) {
            System.out.println(message);
        }
        help(System.out);
    }

    private static void help(PrintStream out) {
        out.println();

        PrintWriter pw = new PrintWriter(out);
        HelpFormatter formatter = new HelpFormatter();
        formatter.setOptionComparator(new CustomComparator());
        formatter.printHelp(pw, HelpFormatter.DEFAULT_WIDTH,
             "amix-token [options] <signing-key>", "\nOptions:", filterOptions(OPTIONS),
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
