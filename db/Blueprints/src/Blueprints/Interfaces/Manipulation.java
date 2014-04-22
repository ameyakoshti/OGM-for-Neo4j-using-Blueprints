package Blueprints.Interfaces;

import com.tinkerpop.frames.EdgeFrame;
import com.tinkerpop.frames.InVertex;
import com.tinkerpop.frames.OutVertex;
import com.tinkerpop.frames.Property;

public interface Manipulation extends EdgeFrame {
	@Property("mid")
	public void setMid(String mid);

	@Property("creatorid")
	public void setCreatorId(String creatorid);

	@Property("rid")
	public void setRid(String rid);

	@Property("modifierid")
	public void setModifierId(String modifierid);

	@Property("timestamp")
	public void setTimestamp(String timestamp);

	@Property("type")
	public void setType(String type);

	@Property("content")
	public void setContent(String content);

	@Property("rid")
	public String getRid();

	@Property("creatorid")
	public String getCreatorId();
	
	@Property("mid")
	public String getMid();
	
	@Property("modifierid")
	public String getModifierId();

	@Property("timestamp")
	public String getTimestamp();

	@Property("type")
	public String getType();

	@Property("content")
	public String getContent();

	@OutVertex
    User getUser();

    @InVertex
    Resource getResource();
}