package Blueprints;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.tooling.GlobalGraphOperations;

import Blueprints.Interfaces.Resource;
import Blueprints.Interfaces.User;

import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.GraphFactory;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.frames.FramedGraphFactory;

import edu.usc.bg.base.ByteIterator;
import edu.usc.bg.base.DB;
import edu.usc.bg.base.ObjectByteIterator;

public class Blueprints extends DB {

	private static final String DB_PROPERTY_PATH = "Blueprints/graph.properties";

	// START SNIPPET: vars
	static Graph graphDB;
	FramedGraphFactory factory;
	FramedGraph<Graph> manager;

	// END SNIPPET: vars

	// START SNIPPET: implement abstract functions
	@Override
	public boolean init() {
		if (graphDB == null) {
			try {
				graphDB = GraphFactory.open(DB_PROPERTY_PATH);
				factory = new FramedGraphFactory();
				manager = factory.create(graphDB);
				//registerShutdownHook(graphDB);
			} catch (Exception e) {
				System.out.println(e);
			}

		}
		return true;
	}

	@Override
	public int insertEntity(String entitySet, String entityPK, HashMap<String, ByteIterator> values, boolean insertImage) {
		if (entitySet == null) {
			return -1;
		}

		// clearDb();

		if (entitySet.equalsIgnoreCase("users")) {
			// for users
			try {
				manager.addVertex(entityPK);
				User user = (User) manager.frame(graphDB.getVertex(entityPK), User.class);
				user.setUserID(entityPK);

				for (Map.Entry<String, ByteIterator> entry : values.entrySet()) {
					if (entry.getKey().equalsIgnoreCase("username"))
						user.setUsername(entry.getValue().toArray().toString());
					if (entry.getKey().equalsIgnoreCase("pw"))
						user.setPw(entry.getValue().toArray().toString());
					if (entry.getKey().equalsIgnoreCase("fname"))
						user.setFName(entry.getValue().toArray().toString());
					if (entry.getKey().equalsIgnoreCase("lname"))
						user.setLName(entry.getValue().toArray().toString());
					if (entry.getKey().equalsIgnoreCase("gender"))
						user.setGender(entry.getValue().toArray().toString());
					if (entry.getKey().equalsIgnoreCase("dob"))
						user.setDOB(entry.getValue().toArray().toString());
					if (entry.getKey().equalsIgnoreCase("jdate"))
						user.setJDate(entry.getValue().toArray().toString());
					if (entry.getKey().equalsIgnoreCase("ldate"))
						user.setLDate(entry.getValue().toArray().toString());
					if (entry.getKey().equalsIgnoreCase("address"))
						user.setAddress(entry.getValue().toArray().toString());
					if (entry.getKey().equalsIgnoreCase("email"))
						user.setEmail(entry.getValue().toArray().toString());
					if (entry.getKey().equalsIgnoreCase("tel"))
						user.setTel(entry.getValue().toArray().toString());
					if (insertImage && entry.getKey().equalsIgnoreCase("tpic"))
						user.setTpic(entry.getValue().toArray().toString());
					if (insertImage && entry.getKey().equalsIgnoreCase("pic"))
						user.setPic(entry.getValue().toString());
				}

			} catch (Exception e) {
				System.out.println("insertEntity Users : " + e.toString());
				return -1;
			}
		} else if (entitySet.equalsIgnoreCase("resources")) {
			// for resources
			try {
				manager.addVertex(entityPK);
				Resource resource = (Resource) manager.frame(graphDB.getVertex(entityPK), Resource.class);
				resource.setRid(entityPK);

				String creatorID = "1";

				for (Map.Entry<String, ByteIterator> entry : values.entrySet()) {
					if (entry.getKey().equalsIgnoreCase("creatorid")) {
						creatorID = entry.getValue().toString();
						resource.setCreatorId(entry.getValue().toString());
					}
					if (entry.getKey().equalsIgnoreCase("walluserid"))
						resource.setWallUserId(entry.getValue().toString());
					if (entry.getKey().equalsIgnoreCase("type"))
						resource.setType(entry.getValue().toString());
					if (entry.getKey().equalsIgnoreCase("body"))
						resource.setBody(entry.getValue().toString());
					if (entry.getKey().equalsIgnoreCase("doc"))
						resource.setDoc(entry.getValue().toString());
				}

				// connect the resource to the user who created it
				User user = (User) manager.frame(graphDB.getVertex(creatorID), User.class);
				user.addResource(resource);

			} catch (Exception e) {
				System.out.println("insertEntity Resources : " + e.toString());
				return -1;
			}
		}

		return 0;
	}

	@Override
	public int viewProfile(int requesterID, int profileOwnerID, HashMap<String, ByteIterator> result, boolean insertImage, boolean testMode) {
		int retVal = 0;

		if (requesterID < 0 || profileOwnerID < 0)
			return -1;

		double frndCount = 0, pendCount = 0, resCount = 0;

		try {

			index = graphDB.index();
			userIndex = index.forNodes("user");

			IndexHits<Node> profileOwnerIndex = userIndex.get("userid", profileOwnerID);
			Node profileOwner = profileOwnerIndex.getSingle();

			// total friends and pending requests of a user
			for (Relationship rel : profileOwner.getRelationships(RelTypes.FRIEND, Direction.BOTH)) {
				if (rel.getProperty("status").equals("accepted")) {
					frndCount++;
				}

				if (rel.getProperty("status").equals("pending")) {
					pendCount++;
				}
			}

			// total resources for a user
			for (Relationship relIterate : profileOwner.getRelationships(RelTypes.OWNS, Direction.BOTH)) {
				resCount++;
			}

			result.put("friendcount", new ObjectByteIterator(Double.toString(frndCount).getBytes()));
			// Pending friend request count.
			// If owner viewing her own profile, she can view her pending
			// friend
			// requests.
			if (requesterID == profileOwnerID) {
				result.put("pendingcount", new ObjectByteIterator(Double.toString(pendCount).getBytes()));
			}
			result.put("resourcecount", new ObjectByteIterator(Double.toString(resCount).getBytes()));

			// put the profile details
			for (String props : profileOwner.getPropertyKeys()) {
				if (!insertImage && (props.toLowerCase().equalsIgnoreCase("tpic") || props.toLowerCase().equalsIgnoreCase("pic")))
					continue;
				result.put(props, new ObjectByteIterator(profileOwner.getProperty(props).toString().getBytes()));
			}

		} catch (Exception e) {
			System.out.println("viewProfile : " + e.toString());
			retVal = -1;
		}

		return retVal;
	}

	// Ankit working on listFriends
	@Override
	public int listFriends(int requesterID, int profileOwnerID, Set<String> fields, Vector<HashMap<String, ByteIterator>> result, boolean insertImage, boolean testMode) {
		System.out.println("Running listFriends()");
		// TODO Auto-generated method stub
		User profileOwner = manager.getVertex(profileOwnerID, User.class);

		for (User friends : profileOwner.getFriends()) {
			HashMap<String, ByteIterator> hm = new HashMap<>();

			if (fields == null || fields.isEmpty()) {
				hm.put("userid", new StringByteIterator(friends.getUserID()));
				hm.put("username", new StringByteIterator(friends.getUsername()));
				hm.put("pw", new StringByteIterator(friends.getPw()));
				hm.put("fname", new StringByteIterator(friends.getFName()));
				hm.put("lname", new StringByteIterator(friends.getLName()));
				hm.put("gender", new StringByteIterator(friends.getGender()));
				hm.put("dob", new StringByteIterator(friends.getDOB()));
				hm.put("jdate", new StringByteIterator(friends.getJDate()));
				hm.put("ldate", new StringByteIterator(friends.getLDate()));
				hm.put("address", new StringByteIterator(friends.getAddress()));
				hm.put("email", new StringByteIterator(friends.getEmail()));
				hm.put("tel", new StringByteIterator(friends.getTel()));
			} else {
				for (String field : fields) {
					if (field.equalsIgnoreCase("userid")) {
						hm.put("userid", new StringByteIterator(friends.getUserID()));
					} else if (field.equalsIgnoreCase("username")) {
						hm.put("username", new StringByteIterator(friends.getUsername()));
					} else if (field.equalsIgnoreCase("pw")) {
						hm.put("pw", new StringByteIterator(friends.getPw()));
					} else if (field.equalsIgnoreCase("fname")) {
						hm.put("fname", new StringByteIterator(friends.getFName()));
					} else if (field.equalsIgnoreCase("lname")) {
						hm.put("lname", new StringByteIterator(friends.getLName()));
					} else if (field.equalsIgnoreCase("gender")) {
						hm.put("gender", new StringByteIterator(friends.getGender()));
					} else if (field.equalsIgnoreCase("dob")) {
						hm.put("dob", new StringByteIterator(friends.getDOB()));
					} else if (field.equalsIgnoreCase("jdate")) {
						hm.put("jdate", new StringByteIterator(friends.getJDate()));
					} else if (field.equalsIgnoreCase("ldate")) {
						hm.put("ldate", new StringByteIterator(friends.getLDate()));
					} else if (field.equalsIgnoreCase("address")) {
						hm.put("address", new StringByteIterator(friends.getAddress()));
					} else if (field.equalsIgnoreCase("email")) {
						hm.put("email", new StringByteIterator(friends.getEmail()));
					} else if (field.equalsIgnoreCase("tel")) {
						hm.put("tel", new StringByteIterator(friends.getTel()));
					}
				}
			}
			if (insertImage) {
				hm.put("pic", new StringByteIterator(friends.getTpic()));
			}
			result.add(hm);
		}
	}

	@Override
	public int viewFriendReq(int profileOwnerID, Vector<HashMap<String, ByteIterator>> results, boolean insertImage, boolean testMode) {
		int retVal = 0;

		if (profileOwnerID < 0)
			return -1;

		try {

			index = graphDB.index();
			userIndex = index.forNodes("user");

			IndexHits<Node> profileOwnerIndex = userIndex.get("userid", profileOwnerID);
			Node profileOwner = profileOwnerIndex.getSingle();

			// total friends and pending requests of a user
			for (Relationship rel : profileOwner.getRelationships(RelTypes.FRIEND, Direction.INCOMING)) {
				// System.out.println("found friend");
				if (rel.getProperty("status").equals("pending")) {
					// System.out.println("found pending");
					Node pendingFriend = rel.getEndNode();
					HashMap<String, ByteIterator> values = new HashMap<String, ByteIterator>();
					for (String props : pendingFriend.getPropertyKeys()) {
						if (!insertImage && (props.toLowerCase().equalsIgnoreCase("tpic") || props.toLowerCase().equalsIgnoreCase("pic")))
							continue;
						// System.out.println(props);
						// System.out.println(pendingFriend.getProperty(props).toString());
						values.put(props, new ObjectByteIterator(pendingFriend.getProperty(props).toString().getBytes()));
					}
					// System.out.println(values.toString());
					results.add(values);
					// System.out.println(results.elementAt(0).toString());
				}
			}

		} catch (Exception e) {
			System.out.println("viewFriendReq : " + e.toString());
			retVal = -1;
		}

		return retVal;
	}

	@Override
	public int acceptFriend(int inviterID, int inviteeID) {
		int retVal = 0;
		if (inviterID < 0 || inviteeID < 0)
			return -1;

		try {
			// System.out.println("In acceptFriend");

			User inviter = (User) manager.frame(graphDB.getVertex(inviterID), User.class);

			User invitee = (User) manager.frame(graphDB.getVertex(inviteeID), User.class);

			if (inviter != null && invitee != null) {
				for (User userReq : invitee.getFriendRequests()) {
					if (userReq.getUserID().equals(inviter.getUserID())) {
						invitee.removeFriendRequests(inviter);
						invitee.addFriend(inviter);
						break;
					}
				}
			}

		} catch (Exception e) {
			System.out.println("acceptFriend : " + e.toString());
			retVal = -1;
		}
		return retVal;
	}

	@Override
	public int rejectFriend(int inviterID, int inviteeID) {
		int retVal = 0;
		if (inviterID < 0 || inviteeID < 0)
			return -1;

		try {

			index = graphDB.index();
			userIndex = index.forNodes("user");

			IndexHits<Node> hitInviter = userIndex.get("userid", inviterID);
			Node inviter = hitInviter.getSingle();

			IndexHits<Node> hitInvitee = userIndex.get("userid", inviteeID);
			Node invitee = hitInvitee.getSingle();

			if (inviter != null && invitee != null) {
				for (Relationship rel : inviter.getRelationships(RelTypes.FRIEND, Direction.BOTH)) {

					// Check if the 2nd user has sent a friend request.
					if (rel.getProperty("status").equals("pending")) {
						Node inviteeFromRel = rel.getEndNode();

						if (Integer.parseInt(inviteeFromRel.getProperty("userid").toString()) == inviteeID) {
							rel.delete();
							break;
						}
					}
				}
			}

		} catch (Exception e) {
			System.out.println("rejectFriend : " + e.toString());
			retVal = -1;
		}
		return retVal;
	}

	@Override
	public int inviteFriend(int inviterID, int inviteeID) {
		int retVal = 0;
		if (inviterID < 0 || inviteeID < 0)
			return -1;

		try {
			// System.out.println("In inviteFriend");

			User inviter = (User) manager.frame(graphDB.getVertex(inviterID), User.class);

			User invitee = (User) manager.frame(graphDB.getVertex(inviteeID), User.class);

			if (inviter != null && invitee != null) {
				inviter.addFriendRequests(invitee);
			}

		} catch (Exception e) {
			System.out.println("inviteFriend : " + e.toString());
			retVal = -1;
		}
		return retVal;
	}

	@Override
	public int viewTopKResources(int requesterID, int profileOwnerID, int k, Vector<HashMap<String, ByteIterator>> result) {

		int retVal = 0;

		if (requesterID < 0 || profileOwnerID < 0 || k < 0)
			return -1;

		int resCount = 0;

		try {

			index = graphDB.index();
			userIndex = index.forNodes("user");

			IndexHits<Node> profileOwnerIndex = userIndex.get("userid", profileOwnerID);
			Node profileOwner = profileOwnerIndex.getSingle();

			for (Relationship rel : profileOwner.getRelationships(RelTypes.OWNS, Direction.BOTH)) {
				resCount++;
				if (resCount > k)
					break;

				Node nodeResource = rel.getEndNode();
				HashMap<String, ByteIterator> values = new HashMap<String, ByteIterator>();
				for (String props : nodeResource.getPropertyKeys()) {
					values.put(props, new ObjectByteIterator(nodeResource.getProperty(props).toString().getBytes()));
				}
				result.add(values);
			}

		} catch (Exception e) {
			System.out.println("viewTopKResources : " + e.toString());
			retVal = -1;
		}

		return retVal;
	}

	@Override
	public int getCreatedResources(int creatorID, Vector<HashMap<String, ByteIterator>> result) {
		int retVal = 0;

		try {

			User member = (User) manager.frame(graphDB.getVertex(creatorID), User.class);

			for (Resource resource : member.getResources()) {
				HashMap<String, ByteIterator> resourceHashMap = new HashMap<String, ByteIterator>();

				resourceHashMap.put("rid", (ByteIterator) resource.getRid());
				resourceHashMap.put("creatorid", (ByteIterator) resource.getCreatorId());
				resourceHashMap.put("walluserid", (ByteIterator) resource.getWallUserId());
				resourceHashMap.put("type", (ByteIterator) resource.getType());
				resourceHashMap.put("body", (ByteIterator) resource.getBody());
				resourceHashMap.put("doc", (ByteIterator) resource.getDoc());

				result.add(resourceHashMap);

			}

		} catch (Exception e) {
			System.out.println("acceptFriend : " + e.toString());
			retVal = -1;
		}
		return retVal;
	}

	@Override
	public int viewCommentOnResource(int requesterID, int profileOwnerID, int resourceID, Vector<HashMap<String, ByteIterator>> result) {

		if (profileOwnerID < 0 || requesterID < 0 || resourceID < 0)
			return -1;

		try {
			index = graphDB.index();
			userIndex = index.forNodes("user");

			IndexHits<Node> hits = userIndex.get("userid", profileOwnerID);
			Node profileOwner = hits.getSingle();

			// System.out.println("finding resource node...");
			for (Relationship relIterate : profileOwner.getRelationships(RelTypes.OWNS, Direction.BOTH)) {
				Node resource = relIterate.getEndNode();

				// find the particular resource on which comments are made
				if (Integer.parseInt(resource.getProperty("rid").toString()) == resourceID) {
					// System.out.println("find manipulation node...");
					// get all the comments on the resource
					for (Relationship relcomments : resource.getRelationships(RelTypes.TO, Direction.INCOMING)) {
						Node manipulation = relcomments.getEndNode();
						HashMap<String, ByteIterator> values = new HashMap<String, ByteIterator>();
						for (String props : manipulation.getPropertyKeys()) {
							values.put(props, new ObjectByteIterator(manipulation.getProperty(props).toString().getBytes()));
						}
						result.add(values);
					}

					break;
				}
			}

		} catch (Exception ex) {
			System.out.println("viewCommentOnResource :" + ex.getMessage());
			return -1;
		}
		return 0;
	}

	@Override
	public int postCommentOnResource(int commentCreatorID, int resourceCreatorID, int resourceID, HashMap<String, ByteIterator> values) {
		try {
			index = graphDB.index();
			userIndex = index.forNodes("user");

			IndexHits<Node> hits = userIndex.get("userid", commentCreatorID);
			Node createdUser = hits.getSingle();

			// System.out.println("finding resource node...");
			for (Relationship relIterate : createdUser.getRelationships(RelTypes.OWNS, Direction.OUTGOING)) {
				Node resource = relIterate.getEndNode();

				if (Integer.parseInt(resource.getProperty("rid").toString()) == resourceID) {
					// create a manipulation node and later connect it to the
					// resource
					// System.out.println("creating manipulation node...");
					Node manipulation = graphDB.createNode();
					manipulation.addLabel(NodeTypes.MANIPULATION);
					for (Map.Entry<String, ByteIterator> entry : values.entrySet()) {
						manipulation.setProperty(entry.getKey().toString(), entry.getValue().toString());
					}
					// System.out.println("adding relationships...");
					// add a relationship from user to manipulation and from
					// manipulation to the resource
					relationship = createdUser.createRelationshipTo(manipulation, RelTypes.MAKES);
					relationship = manipulation.createRelationshipTo(resource, RelTypes.TO);

					break;
				}
			}

		} catch (Exception ex) {
			System.out.println("postCommentOnResource :" + ex.getMessage());
			return -1;
		}
		return 0;
	}

	@Override
	public int delCommentOnResource(int resourceCreatorID, int resourceID, int manipulationID) {

		if (resourceCreatorID < 0 || manipulationID < 0 || resourceID < 0)
			return -1;

		try {

			// System.out.println("finding manipulation node...");
			for (Node node : GlobalGraphOperations.at(graphDB).getAllNodesWithLabel(NodeTypes.MANIPULATION)) {
				// System.out.println("deleting manipulation node...");
				if (Integer.parseInt(node.getProperty("mid").toString()) == manipulationID) {
					// System.out.println("deleting node relationships...");
					for (Relationship relIterate : node.getRelationships(Direction.BOTH)) {
						relIterate.delete();
					}
					node.delete();
					break;
				}
			}

		} catch (Exception ex) {
			System.out.println("delCommentOnResource :" + ex.getMessage());
			return -1;
		}
		return 0;
	}

	@Override
	public int thawFriendship(int friendid1, int friendid2) {
		int retVal = 0;
		if (friendid1 < 0 || friendid2 < 0)
			return -1;

		try {
			index = graphDB.index();
			userIndex = index.forNodes("user");

			IndexHits<Node> hitInviter = userIndex.get("userid", friendid1);
			Node inviter = hitInviter.getSingle();

			IndexHits<Node> hitInvitee = userIndex.get("userid", friendid2);
			Node invitee = hitInvitee.getSingle();

			if (inviter != null && invitee != null) {
				for (Relationship rel : inviter.getRelationships(RelTypes.FRIEND, Direction.BOTH)) {

					// Check if the 2nd user is a friend.
					if (rel.getProperty("status").equals("accepted")) {
						Node inviteeFromRel = rel.getEndNode();

						if (Integer.parseInt(inviteeFromRel.getProperty("userid").toString()) == friendid2) {
							rel.delete();
							break;
						}
					}
				}
			}

		} catch (Exception e) {
			System.out.println("thawFriendship : " + e.toString());
			retVal = -1;
		}
		return retVal;
	}

	@Override
	public HashMap<String, String> getInitialStats() {

		HashMap<String, String> stats = new HashMap<String, String>();
		double usercnt = 0, frndCount = 0, pendCount = 0, resCount = 0;
		double totalFriendsForAll = 0, totalFriendsPendingForAll = 0;

		try {

			// Total number of users
			Iterable<Vertex> allVertices = graphDB.getVertices();
			for (Vertex vertex : allVertices) {
				for (String s : vertex.getPropertyKeys()) {
					if (s.equals("userid")) {
						usercnt++;

						User node = (User) vertex;
						
						// total friends and pending requests of a user
						frndCount = 0;
						pendCount = 0;
						for (User friend : node.getFriendRequests()) {
							pendCount++;
						}
						for (User friend : node.getFriends()) {
							frndCount++;
						}
						for (Resource resource : node.getResources()) {
							resCount++;
						}
						totalFriendsForAll += frndCount;
						totalFriendsPendingForAll += frndCount;
					}
					
					// break is outside the if cos the first property identifies the node type : user or resource
					break;
				}
			}

			frndCount = totalFriendsForAll / usercnt;
			pendCount = totalFriendsPendingForAll / usercnt;
			resCount = resCount / usercnt;

		} catch (Exception e) {
			System.out.println("getInitialStats : " + e.toString());
		}

		stats.put("usercount", Double.toString(usercnt));
		stats.put("avgfriendsperuser", Double.toString(frndCount));
		stats.put("avgpendingperuser", Double.toString(pendCount));
		stats.put("resourcesperuser", Double.toString(resCount));

		return stats;
	}

	@Override
	public int CreateFriendship(int friendid1, int friendid2) {
		int retVal = acceptFriend(friendid1, friendid2);
		return retVal;
	}

	@Override
	public void createSchema(Properties props) {
		// not required for graph db

	}

	@Override
	public int queryPendingFriendshipIds(int memberID, Vector<Integer> pendingIds) {
		int retVal = 0;

		try {

			User member = (User) manager.frame(graphDB.getVertex(memberID), User.class);

			for (User friend : member.getFriendRequests()) {
				pendingIds.add(Integer.parseInt(friend.getUserID()));
			}

		} catch (Exception e) {
			System.out.println("acceptFriend : " + e.toString());
			retVal = -1;
		}
		return retVal;
	}

	@Override
	public int queryConfirmedFriendshipIds(int memberID, Vector<Integer> confirmedIds) {
		int retVal = 0;

		try {

			User member = (User) manager.frame(graphDB.getVertex(memberID), User.class);

			for (User friend : member.getFriends()) {
				confirmedIds.add(Integer.parseInt(friend.getUserID()));
			}

		} catch (Exception e) {
			System.out.println("acceptFriend : " + e.toString());
			retVal = -1;
		}
		return retVal;
	}

	// END SNIPPET: implement abstract functions

	private static void registerShutdownHook(final GraphDatabaseService graphDB) {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				graphDB.shutdown();
			}
		});
	}
}
