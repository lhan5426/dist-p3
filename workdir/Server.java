//import java.io.Serializable;
//import java.rmi.Remote;
//import java.rmi.RemoteException;
//import java.rmi.server.UnicastRemoteObject;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;
import java.time.Instant;
//import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;

public class Server {

	public static int[] hardcoded = new int[]{
		3,3,3,3,
		3,3,2,8,
		4,10,10,10,
		10,10,10,7,
			// 10 may be okay for hour 19
		10,10,10,5,
			//try 10 to see if less timeouts
			// 13 12 10 10 originally
			// 1: failed 5 timeout 1 - 11 orig
			// 2: failed 1 timeout 6 - 12 orig

			// exp has 3 5 for 22, may want to bump up to 11
			// last one is 2 2 for normal
		14,11,11,10
	};

	//Interface for function provided to frontend to access backend
	private interface AppOps extends remote {
		boolean queueRequest(Cloud.FrontEndOps.Request var1) throws RemoteException;
		private synchronized int getLength() throws RemoteException;
		private synchronized Request removeHead() throws InterruptedException, RemoteException;
	}

	private class AppQueue extends UnicastRemoteObject implements Server.AppOps {
		private ArrayBlockingQueue<Cloud.FrontEndOps.Request> jobs = new ArrayBlockingQueue<Cloud.FrontEndOps.Request>(5);

		public AppQueue() throws RemoteException {
			super(0);
		}

		boolean queueRequest(Cloud.FrontEndOps.Request var1) throws RemoteException {
			try {
				this.notifyAll();
				this.jobs.add(var1);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}

		private synchronized int getLength() {
			try {
				return this.jobs.size();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}

		private synchronized Request removeHead() {
			Request r = null;

			try {
				r = (Request)this.jobs.take();
			}
			catch (InterruptedException e1) {
				e1.printStackTrace();
			} catch (RemoteException e2) {
				e2.printStackTrace();
			}
			return r;
		}
	}

	public static String timestamp_log(String s) throws Exception {
		try {
			Instant instant = Instant.now();
			long timestampMilli = instant.toEpochMilli();
			return (s + "\t" + String.valueOf(timestampMilli) + "\n");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "Failed to convert";
	}


	public static void main ( String args[] ) throws Exception {
		//Filehandler setup; Based on SO link: https://tinyurl.com/wnr73bnh
		/*
		String logname = String.join("-", args);
		Logger logger = Logger.getLogger(logname);
		FileHandler fh;

		try {
			// This block configure the logger with handler and formatter
			fh = new FileHandler("/afs/andrew.cmu.edu/usr2/lawrench/private/440/15440-p3/workdir/logs/" + logname);
			logger.addHandler(fh);
			SimpleFormatter formatter = new SimpleFormatter();
			fh.setFormatter(formatter);

			logger.info("----------------Init logs---------------\n");

		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		 */


		if (args.length != 3) throw new Exception("Need 3 args: <cloud_ip> <cloud_port> <VM id>");
		// convert strings
		int ipaddy = -1;
		int port = -1;
		int id = -1;
		try {
			port = Integer.parseInt(args[1]);
			id = Integer.parseInt(args[2]);
		} catch (NumberFormatException e) {
			/*
			logger.info(timestamp_log("IP address: " + args[0]));
			logger.info(timestamp_log("Port: " + args[1]));
			logger.info(timestamp_log("ID: " + args[2]));
			 */
			e.printStackTrace();
		}
/*
		logger.info("IP: " + args[0]);
		logger.info("Port: " + args[1]);
		logger.info("ID: " + args[2]);

		logger.info("Stored Port: " + String.valueOf(port));
		logger.info("ID: " + String.valueOf(id));
*/
		ServerLib SL = new ServerLib( args[0], port );
		int upscale_thres = 0;
		int downscale_thres = 0;
		// right now rolling avg is last 5 interarrival times
		ArrayBlockingQueue<Float> last_times = new ArrayBlockingQueue<Float>(5);
		int endind = 0;
		// total includes master node so technically should never be below 1
		int curr_total = hardcoded[(int) SL.getTime()];
		//logger.info("val: " + String.valueOf(hardcoded[(int) SL.getTime()]));
		//logger.info("ind: " + String.valueOf((int) SL.getTime()));

		//designated master node, started required # of VMs
		//can temporarily act as monolith and handle queued requests
		if (id == 1) {
			//or (int i = 0; i < temp; i ++) {

			for (int i = 0; i < curr_total; i ++) {
				SL.startVM();
			}

			int qextras = SL.getQueueLength();
			/*
			while (qextras > 2) {
				SL.dropHead();
				qextras -= 1;
			}

			 */
			// While other VM's boot, we handle initial queued requests in master node
			// Cannot stay in req handling mode longer than 10 seconds according to Piazza
			while (SL.getTime() < 10) {
				SL.register_frontend();
				Cloud.FrontEndOps.Request r = SL.getNextRequest();
				last_times.add(SL.getTime());
				/*
				if (get_avg(last_times, endind) > 1) {
					SL.startVM();
				}

				 */
				SL.unregister_frontend();
				SL.processRequest(r);
			}
		}
		//TODO measure interarrival times;

		//qlength < 2 means server is keeping up, otherwise
		//this server is not keeping up
		//bool qlong = false;

		int num_front = ((int) Math.floor(hardcoded[(int) SL.getTime()]/2))+1;

		//front end designated
		if (id > 1 && id < num_front+1) {
			// the size of Q needs to be fine tuned probably
			// TODO figure out what args[0] is and change it to less magic
			// TODO error handling for this casting
			AppQueue from_front = (AppQueue) Naming.lookup("//" + args[0] + ":" + port + "/Cloud");
			SL.register_frontend();
			while (true) {
				Cloud.FrontEndOps.Request r = SL.getNextRequest();
				// if job is taking a long time and this put never happens
				// may need some form of timing benchmark to decide when to drop job
				from_front.queueRequest(r);
				//somehow do some RMI shit and send
			}
		//app server
		} else {
			//Each backend needs to create its own threadsafe queue in addition to others
			//Receiving rolling average from each app server may also be good
			//Somehow get req from RMI
			AppQueue to_mid = new AppQueue;
			Naming.rebind("//localhost:" + port + "/Cloud", to_mid);

			while (true) {
				Cloud.FrontEndOps.Request r = AppQueue.removeHead();
				SL.processRequest(r);
			}
		}

		/*
		while (true) {
			SL.register_frontend();
			Cloud.FrontEndOps.Request r = SL.getNextRequest();
			SL.unregister_frontend();
			SL.processRequest(r);
		}
		 */

		/*
		// register with load balancer so requests are sent to this server
		SL.register_frontend();
		// how do i know how many to start?
		for (int i = 0; i < 30; i++) {
			int newid = SL.startVM();
			//SL.doFrontEndWork(newid);
			Cloud.FrontEndOps.Request r = SL.getNextRequest();
			SL.processRequest(r);
		}

		 */
	}
}

