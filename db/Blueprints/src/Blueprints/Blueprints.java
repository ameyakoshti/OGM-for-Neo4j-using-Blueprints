package Blueprints;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

import Blueprints.Interfaces.Manipulation;
import Blueprints.Interfaces.Resource;
import Blueprints.Interfaces.User;

import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.frames.FramedGraph;
import com.tinkerpop.frames.FramedGraphFactory;

import edu.usc.bg.base.ByteIterator;
import edu.usc.bg.base.DB;
import edu.usc.bg.base.ObjectByteIterator;
import edu.usc.bg.base.StringByteIterator;

public class Blueprints extends DB {

	private static final String DB_PROPERTY_PATH = "graph.properties";

	// START SNIPPET: vars
	static Graph graphDB;
	static FramedGraphFactory factory;
	static FramedGraph<Graph> manager;
	static int maxUsers;

	// END SNIPPET: vars

	// START SNIPPET: implement abstract functions
	@Override
	public boolean init() {
		if (graphDB == null) {
			try {
				//graphDB = GraphFactory.open(DB_PROPERTY_PATH);
				graphDB = new Neo4jGraph("db/Blueprints/neo4jdb");
				factory = new FramedGraphFactory();
				manager = factory.create(graphDB);
				maxUsers = 0;
				System.out.println("inside init");
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
		System.out.println("inside insert");
		if (entitySet.equalsIgnoreCase("users")) {
			// for users
			maxUsers++;
			System.out.println("creating user" + entityPK);
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
			int entityPKtemp = maxUsers + Integer.parseInt(entityPK);
			entityPK = "" + entityPKtemp;
			System.out.println("creating resource" + entityPK);
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
				// System.out.println("resource not added to user");
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
			User profileOwner = (User) manager.frame(graphDB.getVertex(profileOwnerID), User.class);

			// total friends and pending requests of a user
			for (User friend : profileOwner.getFriendRequests()) {
				pendCount++;
			}
			for (User friend : profileOwner.getFriends()) {
				frndCount++;
			}
			// total resources for a user
			for (Resource resource : profileOwner.getResources()) {
				resCount++;
			}

			result.put("friendcount", new ObjectByteIterator(Double.toString(frndCount).getBytes()));
			// Pending friend request count.
			// If owner viewing her own profile, she can view her pending friend
			// requests.
			if (requesterID == profileOwnerID) {
				result.put("pendingcount", new ObjectByteIterator(Double.toString(pendCount).getBytes()));
			}
			result.put("resourcecount", new ObjectByteIterator(Double.toString(resCount).getBytes()));

			// put the profile details
			result.put("userid", new StringByteIterator(profileOwner.getUserID()));
			result.put("username", new StringByteIterator(profileOwner.getUsername()));
			result.put("pw", new StringByteIterator(profileOwner.getPw()));
			result.put("fname", new StringByteIterator(profileOwner.getFName()));
			result.put("lname", new StringByteIterator(profileOwner.getLName()));
			result.put("gender", new StringByteIterator(profileOwner.getGender()));
			result.put("dob", new StringByteIterator(profileOwner.getDOB()));
			result.put("jdate", new StringByteIterator(profileOwner.getJDate()));
			result.put("ldate", new StringByteIterator(profileOwner.getLDate()));
			result.put("address", new StringByteIterator(profileOwner.getAddress()));
			result.put("email", new StringByteIterator(profileOwner.getEmail()));
			result.put("tel", new StringByteIterator(profileOwner.getTel()));
			if (insertImage) {
				result.put("tpic", new StringByteIterator(profileOwner.getTpic()));
				result.put("pic", new StringByteIterator(profileOwner.getPic()));
			}
		} catch (Exception e) {
			System.out.println("viewProfile : " + e.toString());
			retVal = -1;
		}

		return retVal;
	}

	@Override
	public int listFriends(int requesterID, int profileOwnerID, Set<String> fields, Vector<HashMap<String, ByteIterator>> result, boolean insertImage, boolean testMode) {
		System.out.println("Running listFriends()");
		int retVal = 0;
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
		return retVal;
	}

	@Override
	public int viewFriendReq(int profileOwnerID, Vector<HashMap<String, ByteIterator>> results, boolean insertImage, boolean testMode) {
		int retVal = 0;

		if (profileOwnerID < 0)
			return -1;

		try {
			User profileOwner = (User) manager.frame(graphDB.getVertex(profileOwnerID), User.class);

			// total friends and pending requests of a user
			for (User pendingFriend : profileOwner.getFriendRequests()) {
				HashMap<String, ByteIterator> values = new HashMap<String, ByteIterator>();

				values.put("userid", new StringByteIterator(pendingFriend.getUserID()));
				values.put("username", new StringByteIterator(pendingFriend.getUsername()));
				values.put("pw", new StringByteIterator(pendingFriend.getPw()));
				values.put("fname", new StringByteIterator(pendingFriend.getFName()));
				values.put("lname", new StringByteIterator(pendingFriend.getLName()));
				values.put("gender", new StringByteIterator(pendingFriend.getGender()));
				values.put("dob", new StringByteIterator(pendingFriend.getDOB()));
				values.put("jdate", new StringByteIterator(pendingFriend.getJDate()));
				values.put("ldate", new StringByteIterator(pendingFriend.getLDate()));
				values.put("address", new StringByteIterator(pendingFriend.getAddress()));
				values.put("email", new StringByteIterator(pendingFriend.getEmail()));
				values.put("tel", new StringByteIterator(pendingFriend.getTel()));
				if (insertImage) {
					values.put("tpic", new StringByteIterator(pendingFriend.getTpic()));
				}

				results.add(values);
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
		//System.out.println("inside acceptFriend");
		try {
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
			User inviter = (User) manager.frame(graphDB.getVertex(inviterID), User.class);

			User invitee = (User) manager.frame(graphDB.getVertex(inviteeID), User.class);

			if (inviter != null && invitee != null) {
				for (User userReq : invitee.getFriends()) {
					// Check if the 2nd user has sent a friend request.
					if (userReq.getUserID().equals(inviter.getUserID())) {
						invitee.removeFriendRequests(inviter);
						break;
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
		//System.out.println("inside inviteFriend");
		try {
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
			User profileOwner = (User) manager.frame(graphDB.getVertex(profileOwnerID), User.class);

			for (Resource resource : profileOwner.getResources()) {
				resCount++;
				if (resCount > k)
					break;

				HashMap<String, ByteIterator> values = new HashMap<String, ByteIterator>();

				values.put("creatorid", new StringByteIterator(resource.getCreatorId()));
				values.put("walluserid", new StringByteIterator(resource.getWallUserId()));
				values.put("type", new StringByteIterator(resource.getType()));
				values.put("body", new StringByteIterator(resource.getBody()));
				values.put("doc", new StringByteIterator(resource.getDoc()));

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

				resourceHashMap.put("rid", new StringByteIterator(resource.getRid()));
				resourceHashMap.put("creatorid", new StringByteIterator(resource.getCreatorId()));
				resourceHashMap.put("walluserid", new StringByteIterator(resource.getWallUserId()));
				resourceHashMap.put("type", new StringByteIterator(resource.getType()));
				resourceHashMap.put("body", new StringByteIterator(resource.getBody()));
				resourceHashMap.put("doc", new StringByteIterator(resource.getDoc()));

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
			//User requester = (User) manager.frame(graphDB.getVertex(requesterID), User.class);
			//User profileOwner = (User) manager.frame(graphDB.getVertex(profileOwnerID), User.class);
			Resource resource = (Resource) manager.frame(graphDB.getVertex(resourceID), Resource.class);
			Iterator<Manipulation> itr = resource.getManipulations().iterator();

			while (itr.hasNext()) {
				Manipulation m = itr.next();
				HashMap<String, ByteIterator> values = new HashMap<String, ByteIterator>();
				values.put("mid", new StringByteIterator(m.getMid()));
				values.put("creatorid", new StringByteIterator(m.getCreatorId()));
				values.put("content", new StringByteIterator(m.getContent()));
				values.put("modifierid", new StringByteIterator(m.getModifierId()));
				values.put("timestamp", new StringByteIterator(m.getTimestamp()));
				values.put("type", new StringByteIterator(m.getType()));
				values.put("rid", new StringByteIterator(resource.getRid()));
				result.add(values);
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

			User commentCreator = (User) manager.frame(graphDB.getVertex(commentCreatorID), User.class);
			//User resourceCreator = (User) manager.frame(graphDB.getVertex(resourceCreatorID), User.class);
			Resource resource = (Resource) manager.frame(graphDB.getVertex(resourceID), Resource.class);
			Manipulation m = (Manipulation) manager.addEdge(values.get("mid").toString(), (Vertex) resource, (Vertex) commentCreator, "manipulation");

			// Manipulation m = resource.addManipulations(commentCreator);

			for (Map.Entry<String, ByteIterator> entry : values.entrySet()) {
				if (entry.getKey().equalsIgnoreCase("content")) {
					m.setContent(entry.getValue().toString());
				}
				if (entry.getKey().equalsIgnoreCase("creatorid")) {
					m.setCreatorId(entry.getValue().toString());
				}
				if (entry.getKey().equalsIgnoreCase("mid")) {
					m.setMid(entry.getValue().toString());
				}
				if (entry.getKey().equalsIgnoreCase("modifierid")) {
					m.setModifierId(entry.getValue().toString());
				}
				if (entry.getKey().equalsIgnoreCase("timestamp")) {
					m.setTimestamp(entry.getValue().toString());
				}
				if (entry.getKey().equalsIgnoreCase("type")) {
					m.setType(entry.getValue().toString());
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

		Manipulation m = (Manipulation) manager.frame(graphDB.getEdge(manipulationID), Manipulation.class);
		Resource resource = (Resource) manager.frame(graphDB.getVertex(resourceID), Resource.class);

		try {
			resource.removeManipulations(m);

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
			User inviter = (User) manager.frame(graphDB.getVertex(friendid1), User.class);

			User invitee = (User) manager.frame(graphDB.getVertex(friendid2), User.class);

			if (inviter != null && invitee != null) {
				for (User userReq : invitee.getFriends()) {
					// Check if the 2nd user is a friend.
					if (userReq.getUserID().equals(inviter.getUserID())) {
						invitee.removeFriend(inviter);
						break;
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

					// break is outside the if cos the first property identifies
					// the node type : user or resource
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
}
