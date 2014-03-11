import java.util.PriorityQueue;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ResourceBundle;
/**
 * 
 * @author Srinivas, Tarun
 * Reference: http://en.literateprograms.org/Special:DownloadCode/Dijkstra%27s_algorithm_(Java)
 * We used this code from the above web site.
 * 
 *
 */
class Vertex implements Comparable<Vertex>
{
	public final String name;
	public Edge[] adjacencies;
	public double minDistance = Double.POSITIVE_INFINITY;
	public Vertex previous;
	public Vertex(String argName) { name = argName; }
	public String toString() { return name; }
	public int compareTo(Vertex other)
	{
		return Double.compare(minDistance, other.minDistance);
	}

}

class Edge
{
	public final Vertex target;
	public final double weight;
	public Edge(Vertex argTarget, double argWeight)
	{ target = argTarget; weight = argWeight; }
}

public class Dijkstra
{
	/**
	 * 
	 * @param source
	 */
	public static final ResourceBundle resourceBundle = ResourceBundle.getBundle("PIMMutlicast");
	public static void computePaths(Vertex source)
	{
		source.minDistance = 0.;
		PriorityQueue<Vertex> vertexQueue = new PriorityQueue<Vertex>();
		vertexQueue.add(source);

		while (!vertexQueue.isEmpty()) {
			Vertex u = vertexQueue.poll();

			// Visit each edge exiting u
			for (Edge e : u.adjacencies)
			{
				Vertex v = e.target;
				double weight = e.weight;
				double distanceThroughU = u.minDistance + weight;
				if (distanceThroughU < v.minDistance) {
					vertexQueue.remove(v);

					v.minDistance = distanceThroughU ;
					v.previous = u;
					vertexQueue.add(v);
				}
			}
		}
	}
	/**
	 * 
	 * @param target
	 * @return
	 */
	public static List<Vertex> getShortestPathTo(Vertex target)
	{
		List<Vertex> path = new ArrayList<Vertex>();
		for (Vertex vertex = target; vertex != null; vertex = vertex.previous)
			path.add(vertex);

		Collections.reverse(path);
		return path;
	}
	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args)
	{
		
		//getDijkstraInfo(4,0);
		List list =  (List) Dijkstra.getDijkstraInfo(Integer.parseInt("4"), Integer.parseInt("0"));
		//Fetch the next hop router
		System.out.println("check it:"+list.get(1));
	}
	/**
	 * 
	 * @param i
	 * @param j
	 * @return
	 */
	public static List<Vertex> getDijkstraInfo(int i, int j) {
		// TODO Auto-generated method stub
		Vertex v0 = new Vertex("R0");
		Vertex v1 = new Vertex("R1");
		Vertex v2 = new Vertex("R2");
		Vertex v3 = new Vertex("R3");
		Vertex v4 = new Vertex("R4");
		Vertex v5 = new Vertex("R5");
		//configTopoList
		String configTopoContent = Router.readFile(resourceBundle.getString("PATH")+"ConfigTopo.props").trim();
		String configTopoList[] = configTopoContent.split("\n");
		String eachItemArr[];
		
		v0.adjacencies = new Edge[]{ new Edge(v1,  1.00),
				new Edge(v2,  4.00) };
		v1.adjacencies = new Edge[]{ new Edge(v0,  1.00),
				new Edge(v2,  2.00),
				new Edge(v3,  1.00) };
		v2.adjacencies = new Edge[]{ new Edge(v0,  4.00), 
				new Edge(v1,  2.00),
				new Edge(v3,  1.00),
				new Edge(v4,  2.00) };														
		v3.adjacencies = new Edge[]{ new Edge(v1,  1.00),
				new Edge(v2,  1.00),
				new Edge(v4,  3.00) };
		v4.adjacencies = new Edge[]{ new Edge(v2,  2.00),
				new Edge(v3,  3.00) };
		Vertex[] vertices = { v0, v1, v2, v3, v4 };

		computePaths(vertices[i]);
		List<Vertex> path = getShortestPathTo(vertices[j]);
		System.out.println("Path: " + path);
		return path;
	}
}
