import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;

public interface Users {

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
	public void getUserID();

	@Property("username")
	public void getUsername();

	@Property("pw")
	public void getPw();

	@Property("fname")
	public void getFName();

	@Property("lname")
	public void getLName();

	@Property("gender")
	public void getGender();

	@Property("dob")
	public void getDOB();

	@Property("jdate")
	public void getJDate();

	@Property("ldate")
	public void getLDate();

	@Property("address")
	public void getAddress();

	@Property("email")
	public void getEmail();

	@Property("tel")
	public void getTel();

	@Property("pic")
	public void getPic();

	@Property("tpic")
	public void getTpic();

	// Creating Edges
	@Adjacency(label = "friend")
	public Iterable<Users> getFriends();

	@Adjacency(label = "friend")
	public void addFriend(Users user);

	@Adjacency(label = "owns")
	public Iterable<Resource> getResources();

	@Adjacency(label = "owns")
	public void addResource(Resource resource);
}