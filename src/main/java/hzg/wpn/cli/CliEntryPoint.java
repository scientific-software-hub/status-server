/*
 * The main contributor to this project is Institute of Materials Research,
 * Helmholtz-Zentrum Geesthacht,
 * Germany.
 *
 * This project is a contribution of the Helmholtz Association Centres and
 * Technische Universitaet Muenchen to the ESS Design Update Phase.
 *
 * The project's funding reference is FKZ05E11CG1.
 *
 * Copyright (c) 2012. Institute of Materials Research,
 * Helmholtz-Zentrum Geesthacht,
 * Germany.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */

package hzg.wpn.cli;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.apache.commons.cli.*;
import org.apache.log4j.Logger;

import java.lang.reflect.Field;
import java.util.Arrays;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 14.05.12
 */
public class CliEntryPoint<T> {
    private final T optionsContainer;
    private final Parser parser;
    private final Options options = new Options();
    private final HelpFormatter help = new HelpFormatter();


    public CliEntryPoint(T optionsContainer, Parser parser) {
        this.optionsContainer = optionsContainer;
        this.parser = parser;
    }

    private volatile boolean initialized;

    /**
     * Call this method after instance creation.
     */
    public void initialize() {
        if (initialized) return;
        for (Field fld : Iterables.<Field>filter(Arrays.asList(optionsContainer.getClass().getDeclaredFields()), new Predicate<Field>() {
            @Override
            public boolean apply(Field input) {
                return input.isAnnotationPresent(CliOption.class);
            }
        })) {
            CliOption cliOption = fld.getAnnotation(CliOption.class);
            options.addOption(cliOption.opt(), cliOption.longOpt(), cliOption.hasArg(), cliOption.description());
        }
        initialized = true;
    }

    /**
     * Parses the command line arguments using parser.
     *
     * @param parser parser
     * @param logger
     * @param args   command line  @throws RuntimeException in case any trouble
     */
    public void parse(Parser parser, Logger logger, String... args) {
        Preconditions.checkState(initialized);
        try {
            CommandLine cl = parser.parser.parse(options, args);
            for (Field fld : Iterables.<Field>filter(Arrays.asList(optionsContainer.getClass().getDeclaredFields()), new Predicate<Field>() {
                @Override
                public boolean apply(Field input) {
                    return input.isAnnotationPresent(CliOption.class);
                }
            })) {
                CliOption cliOption = fld.getAnnotation(CliOption.class);
                try {
                    fld.setAccessible(true);
                    String opt = cliOption.longOpt();
                    if (cl.hasOption(opt))
                        if (cliOption.hasArg()) {
                            String optionValue = cl.getOptionValue(cliOption.opt());
                            logger.info(opt + "=" + optionValue);
                            fld.set(optionsContainer, optionValue);
                        } else {
                            fld.set(optionsContainer, true);
                        }
                } catch (IllegalAccessException e) {
                    logger.error(e.getMessage(), e);
                    throw new RuntimeException(e);
                } finally {
                    fld.setAccessible(false);
                }
            }
        } catch (ParseException e) {
            logger.error("Can not parse options:", e);
            printHelp();
            throw new RuntimeException(e);
        }
    }

    public void printHelp() {
        Preconditions.checkState(initialized);
        help.printHelp("Application supports the following options:", options, true);
    }

    public void parse(Logger logger, String... args) {
        parse(parser, logger, args);
    }

    public static enum Parser {
        POSIX(new PosixParser()),
        GNU(new GnuParser());

        private final CommandLineParser parser;

        private Parser(CommandLineParser parser) {
            this.parser = parser;
        }
    }
}
