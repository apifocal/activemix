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
package org.apifocal.activemx.tools.ping;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * JMS Ping utility for Apache ActiveMQ
 */
public class PingApp {

    private static final String DEFAULT_PING_DESTINATION = "queue://jms.ping";

    private static final Option OPTION_HELP = Option
        .builder("h").longOpt("help").build();
    private static final Option OPTION_USER = Option
        .builder("u").longOpt("user").desc("User for JMS connection").hasArg(true).argName("user").build();
    private static final Option OPTION_PASS = Option
        .builder("p").longOpt("password").desc("Password for JMS connection").hasArg(true).argName("pass").build();
    private static final Option OPTION_LEN = Option
        .builder("l").longOpt("length").desc("Send message of 'len' bytes with random content").hasArg(true).argName("len").build();
    private static final Option OPTION_COUNT = Option
        .builder("c").desc("Stop after sending 'count' messages.").hasArg(true).argName("count").build();
    private static final Option OPTION_INTERVAL = Option
        .builder("i").desc("Wait 'interval' seconds between sending messages").hasArg(true).argName("interval").build();
    private static final Option OPTION_ASYNC = Option
        .builder("a").longOpt("async").desc("Send next ping before receiving reply").hasArg(false).build();
    private static final Option OPTION_NOTHROTTLE = Option
        .builder().longOpt("no-throttle").desc("[NO HELP] Saturate broker; use with care!").build();
    private static final Option OPTION_TTL = Option
        .builder().longOpt("ttl").desc("[NO HELP] Message expiration time").hasArg(true).argName("ttl").build();

    private static final Options OPTIONS = new Options();
    private static final Option[] OPTIONS_LIST = {
        OPTION_HELP,
        OPTION_USER,
        OPTION_PASS,
        OPTION_LEN,
        OPTION_COUNT,
        OPTION_INTERVAL,
        OPTION_ASYNC,
        OPTION_NOTHROTTLE,
        OPTION_TTL,
    };
    private static final String OPTIONS_FOOTER = "\n"
        + " broker-url             JMS connection URL\n"
        + " destination            JMS destination (either queue or topic) as url\n"
        + "                        (e.g. \"queue://queue.name\" \"topic://topic.name\")\n"
        + "                        default: \"queue://jms.ping.<random>\"\n\n"
        + "For more information visit https://docs.silkmq.com";
    static {
        Arrays.asList(OPTIONS_LIST).forEach(o -> {
            OPTIONS.addOption(o);
        });
    };

    public static void main(String[] args) {
        //cleanup
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    // System.out.printf("\n--- %s ping statistics ---\n", "");
                } catch (Exception ex) {
                    System.err.printf("Cannot close jms connection\n");
                }
            }
        });

        CommandLine cli = null;
        try {
            cli = parse(args);
            boolean missingArgs = cli.getArgList().size() < 1 || cli.getArgList().size() > 2;
            if (missingArgs && !cli.hasOption("h")) {
                throw new ParseException("Missing provider url");
            }
        } catch (ParseException e) {
            usage(e.getLocalizedMessage());
            return;
        }

        if (cli.hasOption("h")) {
            help(System.out);
            return;
        }

        List<String> params = cli.getArgList();
        String dn = params.size() > 1 ? params.get(1) : DEFAULT_PING_DESTINATION;
        Pinger pinger = new Pinger(params.get(0), dn);
        pinger.credentials(cli.getOptionValue("u"), cli.getOptionValue("p"));
        int len = 100;
        if (cli.hasOption("l")) {
            len = Integer.parseInt(cli.getOptionValue("l"));
        }
        pinger.randomData(len);
        if (cli.hasOption("c")) {
            pinger.setCount(Integer.parseInt(cli.getOptionValue("c")));
        }
        if (cli.hasOption("i")) {
            pinger.setInterval(Integer.parseInt(cli.getOptionValue("i")));
        }
        if (cli.hasOption("ttl")) {
            pinger.setTtl(Integer.parseInt(cli.getOptionValue("ttl")));
        }
        pinger.setAsync(cli.hasOption("a"));
        pinger.noThrottle(cli.hasOption("no-throttle"));
        
        try {
            pinger.start();
            pinger.run();
            pinger.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
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
             "amx-ping [options] <broker-url> [destination]", "\nOptions:", filterOptions(OPTIONS),
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
                case 'p': result = 3; break;
                case 'l': result = 4; break;
                case 'c': result = 5; break;
                case 'i': result = 6; break;
                case 'a': result = 7; break;
                default: result = 16;
            }
            return result;
        }
    }
}
