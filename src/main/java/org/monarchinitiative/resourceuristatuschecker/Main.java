package org.monarchinitiative.resourceuristatuschecker;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.log4j.Logger;

public class Main {
	private static final Logger logger = Logger.getLogger(Main.class.getName());

	public static void main(String[] args) {
		try {
			Option opt1 = Option.builder("i").argName("i").hasArg().desc("input file path").build();
			Options options = new Options();
			options.addOption(opt1);
			
			CommandLineParser parser = new DefaultParser();
			CommandLine cmdLine = parser.parse(options, args);
			long startTime = System.currentTimeMillis();

			if (cmdLine.hasOption("i")) {
				String inputFilePath = cmdLine.getOptionValue("i");
				ResourceURIStatusChecker rc = new ResourceURIStatusChecker();
				rc.run(inputFilePath);
			} else {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("ResourceURIStatusChecker", options);
			}

			long stopTime = System.currentTimeMillis();
			long elapsedTime = stopTime - startTime;
			logger.info("A whole elapsed time: " + elapsedTime + " milliseconds.");
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
}