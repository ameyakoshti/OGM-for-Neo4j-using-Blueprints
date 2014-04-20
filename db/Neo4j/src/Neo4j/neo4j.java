package Neo4j;

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
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.tooling.GlobalGraphOperations;

import edu.usc.bg.base.ByteIterator;
import edu.usc.bg.base.DB;
import edu.usc.bg.base.ObjectByteIterator;

public class neo4j extends DB {

	private static final String DB_PATH = "db/Neo4j/target/neo4j-db";

	public String greeting;

	// START SNIPPET: vars
	static GraphDatabaseService graphDb;
	Relationship relationship;
	IndexManager index;
	Index<Node> userIndex;

	Node user;
	Node resource;
	Node pendingRequests;
	Node fanPage;
	Node person;
	Node posts;
	Node comments;
	Node photos;

	// END SNIPPET: vars

	// START SNIPPET: createReltype
	private static enum RelTypes implements RelationshipType {
		FRIEND, FOLLOWS, OWNS, ISA, SEND, GET, REPLYOF, HASTTAG, LOCATED, MAKES, TO
	}

	private static enum NodeTypes implements Label {
		USER, RESOURCE, MANIPULATION
	}

	// END SNIPPET: createReltype

	// START SNIPPET: implement abstract functions
	@Override
	public boolean init() {
		if (graphDb == null) {
			graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(DB_PATH);
			registerShutdownHook(graphDb);
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
			// System.out.println("userid : " + entityPK);
			try (Transaction tx = graphDb.beginTx()) {
				index = graphDb.index();
				userIndex = index.forNodes("user");

				user = graphDb.createNode();
				user.addLabel(NodeTypes.USER);
				user.setProperty("userid", entityPK);
				for (Map.Entry<String, ByteIterator> entry : values.entrySet()) {
					if (insertImage && (entry.getKey().equalsIgnoreCase("pic") || entry.getKey().equalsIgnoreCase("tpic"))) {
						user.setProperty(entry.getKey().toString(), entry.getValue().toArray());
					} else {
						user.setProperty(entry.getKey().toString(), entry.getValue().toString());
					}
				}
				userIndex.add(user, "userid", user.getProperty("userid"));

				tx.success();
			} catch (Exception e) {
				System.out.println("insertEntity Users : " + e.toString());
				return -1;
			}
		} else if (entitySet.equalsIgnoreCase("resources")) {
			// for resources
			// System.out.println("rid : " + entityPK);
			try (Transaction tx = graphDb.beginTx()) {
				String creatorID = "1";
				resource = graphDb.createNode();
				resource.addLabel(NodeTypes.RESOURCE);
				resource.setProperty("rid", entityPK);

				for (Map.Entry<String, ByteIterator> entry : values.entrySet()) {
					if (entry.getKey().toString() == "creatorid")
						creatorID = entry.getValue().toString();
					resource.setProperty(entry.getKey().toString(), entry.getValue().toString());
				}

				// connect the resource to the user who created it
				index = graphDb.index();
				userIndex = index.forNodes("user");

				IndexHits<Node> hits = userIndex.get("userid", Integer.parseInt(creatorID));
				Node createdUser = hits.getSingle();

				relationship = createdUser.createRelationshipTo(resource, RelTypes.OWNS);

				tx.success();
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

		try (Transaction tx = graphDb.beginTx()) {

			index = graphDb.index();
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

			tx.success();
		} catch (Exception e) {
			System.out.println("viewProfile : " + e.toString());
			retVal = -1;
		}

		return retVal;
	}

	@Override
	public int listFriends(int requesterID, int profileOwnerID, Set<String> fields, Vector<HashMap<String, ByteIterator>> result, boolean insertImage, boolean testMode) {
		int retVal = 0;
		if (requesterID < 0 || profileOwnerID < 0)
			return -1;

		try (Transaction tx = graphDb.beginTx()) {

			index = graphDb.index();
			userIndex = index.forNodes("user");

			IndexHits<Node> profileOwnerIndex = userIndex.get("userid", profileOwnerID);
			Node profileOwner = profileOwnerIndex.getSingle();

			for (Relationship rel : profileOwner.getRelationships(RelTypes.FRIEND, Direction.BOTH)) {
				if (rel.getProperty("status").toString().equalsIgnoreCase("accepted")) {
					Node friend = rel.getEndNode();
					HashMap<String, ByteIterator> values = new HashMap<String, ByteIterator>();

					if (fields != null) {
						for (String props : friend.getPropertyKeys()) {
							if (fields.contains(props)) {
								if (!insertImage && (props.toLowerCase().equalsIgnoreCase("tpic") || props.toLowerCase().equalsIgnoreCase("pic")))
									continue;
								else {
									if (props.toLowerCase().equals("tpic")) {
										values.put("pic", new ObjectByteIterator(friend.getProperty(props).toString().getBytes()));
									} else {
										values.put(props, new ObjectByteIterator(friend.getProperty(props).toString().getBytes()));
									}
								}
							}
						}
					} else {
						for (String props : friend.getPropertyKeys()) {
							if (!insertImage && (props.toLowerCase().equalsIgnoreCase("tpic") || props.toLowerCase().equalsIgnoreCase("pic")))
								continue;
							values.put(props, new ObjectByteIterator(friend.getProperty(props).toString().getBytes()));
						}
					}
					result.add(values);
				}
			}

			tx.success();
		} catch (Exception e) {
			System.out.println("acceptFriend : " + e.toString());
			retVal = -1;
		}

		return retVal;
	}

	@Override
	public int viewFriendReq(int profileOwnerID, Vector<HashMap<String, ByteIterator>> results, boolean insertImage, boolean testMode) {
		int retVal = 0;

		if (profileOwnerID < 0)
			return -1;

		try (Transaction tx = graphDb.beginTx()) {

			index = graphDb.index();
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

			tx.success();
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

		try (Transaction tx = graphDb.beginTx()) {
			// System.out.println("In acceptFriend");

			index = graphDb.index();
			userIndex = index.forNodes("user");

			IndexHits<Node> hitInviter = userIndex.get("userid", inviterID);
			Node inviter = hitInviter.getSingle();

			IndexHits<Node> hitInvitee = userIndex.get("userid", inviteeID);
			Node invitee = hitInvitee.getSingle();

			if (inviter != null && invitee != null) {
				// relationship = inviter.createRelationshipTo(invitee,
				// RelTypes.FRIEND);
				// System.out.println("from : " + Integer.toString(inviterID) +
				// " to  " + Integer.toString(inviteeID));

				for (Relationship rel : inviter.getRelationships(RelTypes.FRIEND, Direction.BOTH)) {
					// System.out.println("gettin other dude");
					Node inviteeFromRel = rel.getEndNode();

					if (Integer.parseInt(inviteeFromRel.getProperty("userid").toString()) == inviteeID) {
						// System.out.println("found other dude");
						// relationship = inviter.createRelationshipTo(invitee,
						// RelTypes.FRIEND);
						rel.setProperty("status", "accepted");
						break;
					}
				}
			}

			tx.success();
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

		try (Transaction tx = graphDb.beginTx()) {

			index = graphDb.index();
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

			tx.success();
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

		try (Transaction tx = graphDb.beginTx()) {
			// System.out.println("In inviteFriend");

			index = graphDb.index();
			userIndex = index.forNodes("user");

			IndexHits<Node> hitInviter = userIndex.get("userid", inviterID);
			Node inviter = hitInviter.getSingle();

			IndexHits<Node> hitInvitee = userIndex.get("userid", inviteeID);
			Node invitee = hitInvitee.getSingle();

			Relationship relationship;
			if (inviter != null && invitee != null) {
				relationship = inviter.createRelationshipTo(invitee, RelTypes.FRIEND);
				relationship.setProperty("status", "pending");
			}
			tx.success();
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

		try (Transaction tx = graphDb.beginTx()) {

			index = graphDb.index();
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

			tx.success();
		} catch (Exception e) {
			System.out.println("viewTopKResources : " + e.toString());
			retVal = -1;
		}

		return retVal;
	}

	@Override
	public int getCreatedResources(int creatorID, Vector<HashMap<String, ByteIterator>> result) {
		int retVal = 0;

		try (Transaction tx = graphDb.beginTx()) {
			index = graphDb.index();
			userIndex = index.forNodes("user");

			IndexHits<Node> hitInviter = userIndex.get("userid", creatorID);
			Node member = hitInviter.getSingle();

			for (Relationship rel : member.getRelationships(RelTypes.OWNS, Direction.BOTH)) {
				HashMap<String, ByteIterator> resourceHashMap = new HashMap<String, ByteIterator>();

				Node resourceNode = rel.getEndNode();
				resourceHashMap.put("rid", (ByteIterator) resourceNode.getProperty("rid"));
				resourceHashMap.put("creatorid", (ByteIterator) resourceNode.getProperty("creatorid"));
				resourceHashMap.put("walluserid", (ByteIterator) resourceNode.getProperty("walluserid"));
				resourceHashMap.put("type", (ByteIterator) resourceNode.getProperty("type"));
				resourceHashMap.put("body", (ByteIterator) resourceNode.getProperty("body"));
				resourceHashMap.put("doc", (ByteIterator) resourceNode.getProperty("doc"));

				result.add(resourceHashMap);
			}

			tx.success();
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

		try (Transaction tx = graphDb.beginTx()) {
			index = graphDb.index();
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

			tx.success();
		} catch (Exception ex) {
			System.out.println("viewCommentOnResource :" + ex.getMessage());
			return -1;
		}
		return 0;
	}

	@Override
	public int postCommentOnResource(int commentCreatorID, int resourceCreatorID, int resourceID, HashMap<String, ByteIterator> values) {
		try (Transaction tx = graphDb.beginTx()) {
			index = graphDb.index();
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
					Node manipulation = graphDb.createNode();
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

			tx.success();
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

		try (Transaction tx = graphDb.beginTx()) {

			// System.out.println("finding manipulation node...");
			for (Node node : GlobalGraphOperations.at(graphDb).getAllNodesWithLabel(NodeTypes.MANIPULATION)) {
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

			tx.success();
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

		try (Transaction tx = graphDb.beginTx()) {
			index = graphDb.index();
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

			tx.success();
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

		try (Transaction tx = graphDb.beginTx()) {
			// index = graphDb.index();
			// userIndex = index.forNodes("user");

			// CYPHER has a slower execution
			/*
			 * ExecutionEngine engine = new ExecutionEngine(graphDb); String
			 * query = "match (n:USER) return count(*)"; ExecutionResult result
			 * = engine.execute(query); Iterator<Node> it =
			 * result.columnAs("n"); while (it.hasNext()) { Node count =
			 * it.next(); usercnt =
			 * Integer.parseInt(count.getProperty("USER").toString()); }
			 */

			// Total number of users
			for (Node node : GlobalGraphOperations.at(graphDb).getAllNodesWithLabel(NodeTypes.USER)) {
				usercnt++;

				// total friends and pending requests of a user
				frndCount = 0;
				pendCount = 0;
				for (Relationship rel : node.getRelationships(RelTypes.FRIEND, Direction.BOTH)) {
					if (rel.getProperty("status").equals("accepted")) {
						frndCount++;
					}
					if (rel.getProperty("status").equals("pending")) {
						pendCount++;
					}
				}
				totalFriendsForAll += frndCount;
				totalFriendsPendingForAll += frndCount;

				// total resources for a user
				for (Relationship relIterate : node.getRelationships(RelTypes.OWNS, Direction.BOTH)) {
					resCount++;
				}
			}

			frndCount = totalFriendsForAll / usercnt;
			pendCount = totalFriendsPendingForAll / usercnt;
			resCount = resCount / usercnt;
			tx.success();
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

		try (Transaction tx = graphDb.beginTx()) {

			index = graphDb.index();
			userIndex = index.forNodes("user");

			IndexHits<Node> hitInviter = userIndex.get("userid", memberID);
			Node member = hitInviter.getSingle();

			for (Relationship rel : member.getRelationships(RelTypes.FRIEND, Direction.BOTH)) {
				if (rel.getProperty("status").toString().equalsIgnoreCase("pending")) {
					// System.out.println("found friend");
					Node otherDude = rel.getEndNode();
					pendingIds.add(Integer.parseInt(otherDude.getProperty("userid").toString()));
				}
			}

			tx.success();
		} catch (Exception e) {
			System.out.println("acceptFriend : " + e.toString());
			retVal = -1;
		}
		return retVal;
	}

	@Override
	public int queryConfirmedFriendshipIds(int memberID, Vector<Integer> confirmedIds) {
		int retVal = 0;

		try (Transaction tx = graphDb.beginTx()) {

			index = graphDb.index();
			userIndex = index.forNodes("user");

			IndexHits<Node> hitInviter = userIndex.get("userid", memberID);
			Node member = hitInviter.getSingle();

			for (Relationship rel : member.getRelationships(RelTypes.FRIEND, Direction.BOTH)) {
				if (rel.getProperty("status").toString().equalsIgnoreCase("accepted")) {
					// .println("found friend");
					Node otherDude = rel.getEndNode();
					confirmedIds.add(Integer.parseInt(otherDude.getProperty("userid").toString()));
				}
			}

			tx.success();
		} catch (Exception e) {
			System.out.println("acceptFriend : " + e.toString());
			retVal = -1;
		}
		return retVal;
	}

	// END SNIPPET: implement abstract functions

	private static void registerShutdownHook(final GraphDatabaseService graphDb) {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				graphDb.shutdown();
			}
		});
	}
}
