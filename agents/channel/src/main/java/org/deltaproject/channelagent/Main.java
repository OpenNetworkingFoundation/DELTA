package org.deltaproject.channelagent;


import org.deltaproject.channelagent.core.Interface;
import org.slf4j.impl.SimpleLogger;

public class Main {
	public static void main(String args[]) {
		if (args.length != 2) {
			System.err.println("Usage: java -jar delta-agent-channel-x.x-SNAPSHOT-jar-with-dependencies.jar <Agent-Manager IP> <Agent-Manager Port>");
			System.exit(1);
		}

		System.setProperty(SimpleLogger.SHOW_SHORT_LOG_NAME_KEY, "TRUE");
		Interface amInterface = new Interface(args[0], args[1]);
//		amInterface.connectManager();

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		amInterface.start();
	}
}