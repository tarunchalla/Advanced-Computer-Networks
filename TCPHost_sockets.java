

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;
/**
 * 
 * @author Srinivas, Tarun
 * @purpose To create a TCP connection to Router. And sending files, joining, leaving the multicast groups.
 *
 */
public class TCPHost_sockets extends Thread{
	public static final ResourceBundle resourceBundle = ResourceBundle.getBundle("PIMMutlicast");
	private static String message = null;
	public static final HashSet<String> multiCastList = new HashSet<String>();
	
	public static synchronized String getCount(){
	  return message;
	}

	public synchronized void setCount(String count){
		message = count;
	}
	public static AtomicInteger at = new AtomicInteger();
	/**
	 * Here we are handling the different commands issued by the host, processing and sending it to the appropriate Router.
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		
		if(args.length != 5){
			System.out.println("Usage: host <hostID> <configfile> <myrouterID> <mgroup>");
			return;
		}
		else{
			String firstArg;
			final String hostId;
			String configFile;
			String routerId;
			final String mGroupId;
			String configContent;
			
			
			firstArg = args[0];
			hostId = args[1];
			configFile = args[2];
			routerId = args[3];
			mGroupId = args[4];
			if(firstArg.equalsIgnoreCase("host")){
				configContent = readFile(resourceBundle.getString("PATH")+configFile).trim();
				String routerList [] = configContent.split("\n");
				String routerInfo = routerList[Integer.parseInt(routerId)];
				String routerArr[] = routerInfo.split(" ");
				if(routerArr.length >=3){
					String routerIp = routerArr[1];
					int routerPort = Integer.parseInt(routerArr[2]);
					InetAddress routerIPAddr = InetAddress.getByName(routerIp);  // Fetching the IP address from the hostname
					System.out.println(routerIPAddr.toString()+routerPort);
					final Socket routerSocket = new Socket(routerIPAddr, routerPort);
					System.out.println("check bound status:"+routerSocket.isBound()+"  check connection status:"+routerSocket.isConnected()+" isclosed:"+routerSocket.isClosed());

					Thread senderThread  = new Thread(){
						public void run(){
							BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));  //creating object for bufferreader
							String userInput = null;
							PrintWriter pwriter = null;
							try {
								pwriter = new PrintWriter(routerSocket.getOutputStream(), true);
								System.out.println("Router connection established --- Sending REPORT <myID> <mgroup>");
								pwriter.println(String.format("REPORT %s %s", hostId,mGroupId));
								multiCastList.add(mGroupId);
								//System.out.println("Response from router :"+msgFromRouter.readLine());
								while ((userInput = inFromUser.readLine()) != null){ // reading the user Input String 
									if(null != userInput
											&& userInput.split(" ").length >=0){
										String userInputArr[] = userInput.split(" ");
										String checkType = userInputArr[0].trim();
										String mgroupId;

										if(checkType.equalsIgnoreCase("JOIN")
												&& userInputArr.length == 2){
											mgroupId = userInputArr[1];
											//check if it alrady a member of multicast group
											boolean checkMulticastMShip;
											checkMulticastMShip = checkMulticastMemberShip(mgroupId);
											if(!checkMulticastMShip){
												System.out.println("Not a member of group:"+mgroupId+ " so add it.");
												pwriter.println(String.format("REPORT %s %s", hostId,mgroupId));

											}
											else{
												System.out.println("Already a member of the multicast group:"+mgroupId);
											}

										}
										else if(checkType.equalsIgnoreCase("SEND")
												&& userInputArr.length == 3){
											mgroupId = userInputArr[2];
											String file = readFile(resourceBundle.getString("PATH")+userInputArr[1]);
											 byte[] theByteArray = file.getBytes();
											 String byteStr = new String(theByteArray);
											checkMulticastMemberShip(mgroupId);
											//sending though its a member or not 
											pwriter.println(String.format("SEND %s %s %s", hostId,mgroupId,byteStr));
										}
										else if(checkType.equalsIgnoreCase("LEAVE")
												&& userInputArr.length == 2){
											//pwriter.println("LIST");
											//check if it alrady a member of multicast group
											mgroupId = userInputArr[1];

											boolean checkMulticastMShip;
											checkMulticastMShip = checkMulticastMemberShip(mgroupId);
											if(checkMulticastMShip){
												System.out.println("Already a member of the multicast group so leave the group:"+mgroupId);
												pwriter.println(String.format("LEAVE %s %s", hostId,mgroupId));
												boolean checkIsRemoved = removeMulticastMShip(mgroupId);
											}
											else{
												System.out.println("Not a member of group dont do anything:"+mgroupId);
											}

										}
										else if(checkType.equalsIgnoreCase("LIST")
												&& userInputArr.length >= 1){
											System.out.println("user requested LIST");
											StringBuffer mlist= new StringBuffer();
											
											Iterator<String> iter = multiCastList.iterator();
											String eachItem;
											while(iter.hasNext()){
												eachItem = iter.next();
													mlist.append(eachItem+" ");
											}		
											System.out.println(String.format("you are member of multigroup %s", mlist.toString()));
											//pwriter.println("LIST");
										}
										else{
											System.out.println("wrong format");
										}
									}

								}
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							if(pwriter != null)
								pwriter.close();
							try {
								inFromUser.close();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}

						}

						
					};
					senderThread.start();
					//msgFromRouter.close();
					Thread receiverThread = new Thread(){
						public void run(){
							BufferedReader msgFromRouter;
							String msg;
							try {
								msgFromRouter = new BufferedReader(new InputStreamReader(routerSocket.getInputStream()));
								while ((msg = msgFromRouter.readLine()) != null){
									 String messge = new String(msg.getBytes());
									System.out.println("Received Message from Router:"+messge);
									System.out.println("Waiting for messages!!!:");
								}
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}

						}
					};
					receiverThread.start();
					
					//join the threads
					try {
						senderThread.join();
						receiverThread.join();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					routerSocket.close();
				}
				else{
					System.out.println("Error in reading Config file");
				}
			}
			else{
				System.out.println("Usage: host <hostID> <configfile> <myrouterID> <mgroup>");
				return;
			}
		}
	}
	/**
	 * Maintaining the join messages so that we can LIST them to the user.
	 * @param mgroupId
	 * @return
	 */
	private static boolean checkMulticastMemberShip(String mgroupId) {
		// TODO Auto-generated method stub

		boolean isHostMemberOfMGroup=false;
		Iterator<String> iter = multiCastList.iterator();
		String eachItem;
		while(iter.hasNext()){
			eachItem = iter.next();
			if(eachItem.equalsIgnoreCase(mgroupId)){
				System.out.println(" Host is member of multicast group");
				isHostMemberOfMGroup = true;
			}
		}		
		if(!isHostMemberOfMGroup){
			System.out.println("Inside checkMulticastMemberShip ----->New multicast group added!!!!!!");
			multiCastList.add(mgroupId);
		}
		return isHostMemberOfMGroup;
	}
	/**
	 * Maintaining LEAVING history to display LIST
	 * @param mgroupId
	 * @return
	 */
	private static boolean removeMulticastMShip(String mgroupId) {
		// TODO Auto-generated method stub

		boolean isRemoved=false;
		System.out.println("size before:"+multiCastList.size());
		isRemoved = multiCastList.remove(mgroupId);
		System.out.println("size after:"+multiCastList.size());
		
		return isRemoved;
	}
	/**
	 * Method to read the file and fetch the required information.
	 * @param filepath
	 * @return
	 */
	public static String readFile(String filepath) 
	{
		BufferedReader  reader = null;
		File file = new File(filepath);
		FileInputStream fis = null;
		BufferedInputStream bis = null;
		String inputLine = null;
		try {
			fis = new FileInputStream(file);
			bis = new BufferedInputStream(fis);
			reader = new BufferedReader(new InputStreamReader( bis, "UTF-8"));
			StringBuffer buffer = new StringBuffer();
			while ((inputLine = reader.readLine())!= null) {
				buffer.append(inputLine+"\n");
			}
			inputLine = buffer.toString();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return inputLine;

	}
}

