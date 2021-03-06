/*
 * Copyright 2010 the original author or authors.
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

package org.springframework.data.gemfire.samples.helloworld;

import com.gemstone.gemfire.cache.Region;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * Entity processing and interpreting shell commands.
 * 
 * @author Costin Leau
 */
@Component
public class CommandProcessor {

	private static final Pattern COM = Pattern.compile("cancel|query|exit|help|size|clear|keys|values|map|containsKey|containsValue|get|remove|put");

	private static final Log log = LogFactory.getLog(CommandProcessor.class);

	private static String help = initHelp();
	private static String EMPTY = "";

	boolean threadActive;
	private Thread thread;
	private Executor executor;
	Region<String, String> region ;



	public void setRegion(Region<String, String> region) {
		this.region = region;
	}

	void start() {
		if (thread == null) {
			threadActive = true;
			thread = new Thread(new Task(), "cmd-processor");
			thread.start();
			executor = Executors.newFixedThreadPool(50);
		}
	}

	void stop() throws Exception {
		threadActive = false;
		thread.join(3 * 100);
	}

	void awaitCommands() throws Exception {
		thread.join();
	}



	private class Task implements Runnable {

		public void run() {
			System.out.println("Hello World!");
			System.out.println("Want to interact with the world ? ...");
			System.out.println(help);
			System.out.print("-> ");
			System.out.flush();

			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

			try {
				while (threadActive) {
					if (br.ready()) {
						try {
							System.out.println(process(br.readLine()));
						} catch (Exception ex) {
							System.out.println("Error executing last command " + ex.getMessage());
							ex.printStackTrace();
						}

						System.out.print("-> ");
						System.out.flush();
					}
				}
			} catch (IOException ioe) {
				// just ignore any exceptions
				log.error("Caught exception while processing commands ", ioe);
			}
		}
	}

	static boolean notEmpty(String v) {
		return v!=null && !v.isEmpty();
	}

	private static String initHelp() {
		try {
			InputStream stream = CommandProcessor.class.getResourceAsStream("help.txt");
			byte[] buffer = new byte[stream.available() > 0 ? stream.available() : 300];

			BufferedInputStream bf = new BufferedInputStream(stream);
			bf.read(buffer);
			return new String(buffer);
		} catch (IOException io) {
			throw new IllegalStateException("Cannot read help file");
		}
	}

	HashMap<String, ITask> tasks = new HashMap<String, ITask>();

	String data = "kkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkk";
	String process(final String line) {
		final Scanner sc = new Scanner(line);

		if (!sc.hasNext(COM)) {
			return "Invalid command - type 'help' for supported operations";
		}
		String command = sc.next();
		String arg1 = (sc.hasNext() ? sc.next() : null);
		String arg2 = (sc.hasNext() ? sc.next() : null);

		if ("cancel".equalsIgnoreCase(command)) {
			if (arg1!=null) {
				ITask task = tasks.get(arg1);
				if (task!=null) {
					tasks.remove(arg1);
					task.cancel();
				}
			}

		}

		// query shortcut
		if ("query".equalsIgnoreCase(command)) {
			String query = line.trim().substring(command.length());
			try {
				return region.query(query).toString();
			} catch (Exception e) {
				e.printStackTrace();
				return "ERROR";
			}
		}

		// parse commands w/o arguments
		if ("exit".equalsIgnoreCase(command)) {
			threadActive = false;
			return "Node exiting...";
		}
		if ("help".equalsIgnoreCase(command)) {
			return help;
		}
		if ("size".equalsIgnoreCase(command)) {
			if (notEmpty(arg1)) {
				int size = Integer.parseInt(arg1);
				char [] buf = new char[size];
				for (int i=0;i<size;i++) {
					buf[i] = 'a';
				}
				data = new String(buf);
			}
			return EMPTY + region.size();
		}
		if ("clear".equalsIgnoreCase(command)) {
			region.clear();
			return "Clearing grid..";
		}
		if ("keys".equalsIgnoreCase(command)) {
			return region.keySet().toString();
		}
		if ("values".equalsIgnoreCase(command)) {
			return region.values().toString();
		}

		if ("map".equalsIgnoreCase(command)) {
			Set<Entry<String, String>> entrySet = region.entrySet();
			if (entrySet.size() == 0)
				return "[]";

			StringBuilder sb = new StringBuilder();
			for (Entry<String, String> entry : entrySet) {
				sb.append("[");
				sb.append(entry.getKey());
				sb.append("=");
				sb.append(entry.getValue());
				sb.append("] ");
			}
			return sb.toString();
		}

		// commands w/ 1 arg
		if ("containsKey".equalsIgnoreCase(command)) {
			return EMPTY + region.containsKey(arg1);
		}
		if ("containsValue".equalsIgnoreCase(command)) {
			return EMPTY + region.containsValue(arg1);
		}
		if ("get".equalsIgnoreCase(command)) {
			if (tasks.containsKey("get")) {
				System.out.println("You have get task running. !!!");
				return "RUNNING";
			}
			ITask task;
			IterFunc<Void> func = new IterFunc<Void>() {
				public Void apply(Void v, int id) {
					region.get("" + id);
					return null;
				}
			};

			int num = Integer.parseInt(arg1);
			if (arg2!=null && !arg2.isEmpty()) {
				int split = Integer.parseInt(arg2);
				task = new MTask<Void>(split,executor,num,func,null);
			} else {
				task = new ATask<Void>(null,num, 0, func, null);
			}
			tasks.put("get",task);
			executor.execute(task);
			return "OK";
//					return region.get(arg1);
		}
		if ("remove".equalsIgnoreCase(command)) {
			return region.remove(arg1);
		}

		// commands w/ 2 args

		if ("put".equalsIgnoreCase(command)) {
			if (tasks.containsKey("put")) {
				System.out.println("You have PUT task running. !!!");
				return "RUNNING";
			}
			int num = Integer.parseInt(arg1);
			ITask task;
			IterFunc<Void> func = (v, id) -> {
                region.put("" + id, data);
                return null;
            };

			if (arg2!=null && !arg2.isEmpty()) {
				int split = Integer.parseInt(arg2);
				task = new MTask<Void>(split,executor,num,func,null);
			} else {
				task = new ATask<Void>(null,num, 0, func, null);
			}

			tasks.put("put",task);
			executor.execute(task);
			return "OK";
		}

		sc.close();
		return "unknown command - run 'help' for available commands";
	}
}