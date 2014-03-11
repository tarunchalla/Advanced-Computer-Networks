

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ResourceBundle;

/**
 * 
 * @author Srinivas, Tarun
 * We are handling different commands sent by the Host. Processing those commands and sending/forwarding to the appropriate
 * Routers and Hosts. Also updating the routing tables.
 */
public class Router {
	public static final ResourceBundle resourceBundle = ResourceBundle.getBundle("PIMMutlicast");
	//static protected List<PrintWriter> writers = new ArrayList<PrintWriter>();
	/**
	 * Used for Initializing the Router, Creating connections with different hosts and assigning them to different threads.
	 * @param args
	 * @throws IOException
	 */
    public static void main(String[] args) throws IOException {

    	if(args.length != 5){
    		System.out.println("Usage: router <routerID> <configfile> <config-rp> <config-topo>");
    		return;
    	}
    	else{
    		String firstArg;
    		String routerId;
    		String configFile;
    		String configRP;
    		String configTopo;
    		String configContent;
    		String configRPContent;
    		String configTopoContent;
    		
    		firstArg = args[0];
    		routerId = args[1];
    		configFile = args[2];
    		configRP = args[3];
    		configTopo = args[4];
    	//	BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));  //creating object for bufferreader
    			if(firstArg.equalsIgnoreCase("router")){
    				configContent = readFile(resourceBundle.getString("PATH")+configFile).trim();
    				configRPContent = readFile(resourceBundle.getString("PATH")+configRP).trim();
    				configTopoContent = readFile(resourceBundle.getString("PATH")+configTopo).trim();
    				String routerList [] = configContent.split("\n");
    				String rpList[] = configRPContent.split("\n");
    				String configTopoList[] = configTopoContent.split("\n");
    				HashMap<String, String> rpMap = new HashMap<String, String>();
    				String rpArr[];
    				for(int i=0;i<rpList.length;i++){
    					rpArr = rpList[i].split(" ");
    					if(rpArr.length >=2){
    						rpMap.put(rpArr[0], rpArr[1]);
    					}
    				}
    				int noOfRouters = Integer.parseInt(configTopoList[0]);
    				String routerInfo = routerList[Integer.parseInt(routerId)];
    				String routerArr[] = routerInfo.split(" ");
    				if(routerArr.length >=3){
    					String routerIp = routerArr[1];
    					int routerPort = Integer.parseInt(routerArr[2]);
    					ServerSocket serverSocket = new ServerSocket(routerPort);    					
    					Socket socket;
    					while(true){
    						socket = serverSocket.accept();
    						System.out.println("Here creating socket");
    						MultiServerThread connection = new MultiServerThread(socket,routerId,rpMap);
    						System.out.println("creating thread & starting");
                            Thread t = new Thread(connection);
                            t.start();
    					}
    				}
    				else{
    					System.out.println("Error in reading Config file");
    				}
    			}
    			else{
    				System.out.println("Usage: router <routerID> <configfile> <config-rp> <config-topo>");
    				return;
    			}
    		}
    }
    
	public static boolean CheckRouterMemberShipInfo() {
		// TODO Auto-generated method stub
		return false;
	}
	/**
	 * This method is used to read the config files and parse the required information.
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

