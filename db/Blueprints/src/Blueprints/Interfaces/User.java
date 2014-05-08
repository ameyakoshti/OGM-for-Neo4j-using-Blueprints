package Blueprints.Interfaces;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;

public interface User {

	// Setting all the attributes
	@Property("userid")
	public void setUserID(String userid);

	@Property("username")
	public void setUsername(String username);

	@Property("pw")
	public void setPw(String pw);

	@Property("fname")
	public void setFName(String fname);

	@Property("lname")
	public void setLName(String lname);

	@Property("gender")
	public void setGender(String gender);

	@Property("dob")
	public void setDOB(String dob);

	@Property("jdate")
	public void setJDate(String jdate);

	@Property("ldate")
	public void setLDate(String ldate);

	@Property("address")
	public void setAddress(String address);

	@Property("email")
	public void setEmail(String email);

	@Property("tel")
	public void setTel(String tel);

	@Property("pic")
	public void setPic(String pic);

	@Property("tpic")
	public void setTpic(String tpic);

	// Getting all the attributes
	@Property("userid")
	public String getUserID();

	@Property("username")
	public String getUsername();

	@Property("pw")
	public String getPw();

	@Property("fname")
	public String getFName();

	@Property("lname")
	public String getLName();

	@Property("gender")
	public String getGender();

	@Property("dob")
	public String getDOB();

	@Property("jdate")
	public String getJDate();

	@Property("ldate")
	public String getLDate();

	@Property("address")
	public String getAddress();

	@Property("email")
	public String getEmail();

	@Property("tel")
	public String getTel();

	@Property("pic")
	public String getPic();

	@Property("tpic")
	public String getTpic();

	// Setting adjacency
	@Adjacency(label = "friend")
	public Iterable<User> getFriends();

	@Adjacency(label = "friend")
	public void addFriend(User user);

	@Adjacency(label = "friend")
	public void removeFriend(User user);

	@Adjacency(label = "friendRequest")
	public Iterable<User> getFriendRequests();

	@Adjacency(label = "friendRequest", direction = Direction.OUT)
	public void addFriendRequests(User user);

	@Adjacency(label = "friendRequest")
	public void removeFriendRequests(User user);

	@Adjacency(label = "owns")
	public Iterable<Resource> getResources();

	@Adjacency(label = "owns")
	public void addResource(Resource resource);

	@Adjacency(label = "creates")
	public void addManipulation(Manipulation manipulation);

	@Adjacency(label = "creates")
	public void removeManipulation(Manipulation manipulation);

	@Adjacency(label = "creates")
	public Iterable<Manipulation> getManipulation();
}
