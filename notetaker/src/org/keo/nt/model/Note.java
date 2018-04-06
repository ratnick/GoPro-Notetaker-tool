package org.keo.nt.model;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.util.Pair;

public class Note {
	
	private final StringProperty firstName;
    private final StringProperty lastName;
    private final StringProperty noteTaker;
    private final StringProperty respondentId;
    private final StringProperty noteDate;
    private final StringProperty noteBody;
    private final StringProperty videoLocation;
    private final StringProperty questions;
        
    /**
     * Default constructor.
     */
    public Note() {
    	this.firstName = new SimpleStringProperty(null);
        this.lastName = new SimpleStringProperty(null);
        this.noteTaker = new SimpleStringProperty(null);
        this.respondentId = new SimpleStringProperty(null);
        this.noteDate = new SimpleStringProperty(null);  
        this.noteBody = new SimpleStringProperty(null);
        this.videoLocation = new SimpleStringProperty(null);
        this.questions = new SimpleStringProperty(null);
    }
    
    public Boolean isReadyToSave() {
    	if (this.firstName.getValue() == null || this.firstName.getValue().isEmpty())
    		return false;
    	if (this.lastName.getValue() == null || this.lastName.getValue().isEmpty())
    		return false;
    	if (this.noteTaker.getValue() == null || this.noteTaker.getValue().isEmpty())
    		return false;
    	if (this.respondentId.getValue() == null || this.respondentId.getValue().isEmpty())
    		return false;    	
    	
    	return true;
    }
    
    public void setNoteAttributes(List<Pair<String,String>> data) {
    	data.forEach((pair) -> {
        	switch(pair.getKey()) {
        	case "firstname":            		
        		setFirstName(pair.getValue());
        		break;
        	case "lastname":            		
        		setLastName(pair.getValue());
        		break;
        	case "respondentid":            		
        		setRespondentId(pair.getValue());
        		break;
        	case "notedate":            		
        		setNoteDate(pair.getValue());
        		break;
        	case "notetaker":            		
        		setNoteTaker(pair.getValue());
        		break;
        	}
        });
    }
    
    public Boolean loadFile(File file)  {
    	if (file.exists()) {
    		try {
	    		File fXmlFile = new File(file.getPath());
	    		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	    		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();	    	
	    		Document doc = dBuilder.parse(fXmlFile);
			
	    		doc.getDocumentElement().normalize();
	    	 		    		
	    		NodeList nList = doc.getElementsByTagName("notebody");
	    		if (nList.getLength() > 0)
	    			this.setNoteBody(nList.item(0).getTextContent());
	    		
	    		nList = doc.getElementsByTagName("firstname");
	    		if (nList.getLength() > 0)
	    			this.setFirstName(nList.item(0).getTextContent());
	    		
	    		nList = doc.getElementsByTagName("lastname");
	    		if (nList.getLength() > 0)
	    			this.setLastName(nList.item(0).getTextContent());
	    		
	    		nList = doc.getElementsByTagName("videolocation");
	    		if (nList.getLength() > 0)
	    			this.setVideoLocation(nList.item(0).getTextContent());
	    		
	    		nList = doc.getElementsByTagName("questions");
	    		if (nList.getLength() > 0)
	    			this.setQuestions(nList.item(0).getTextContent());
	    		
    		} catch(ParserConfigurationException | SAXException | IOException e) {
    			return false;
    		}
    	} else {
    		return false;
    	}
    	return true;
	}
    
    public Boolean createNewFile(File file) {
    	try {
    		 
    		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
    		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
     
    		// root elements
    		Document doc = docBuilder.newDocument();
    		Element root = doc.createElement("note");
    		doc.appendChild(root);
    		{
	    		Element respondent = doc.createElement("respondent");
	    		root.appendChild(respondent);
	    		{
	    			Element firstname = doc.createElement("firstname");
		    		respondent.appendChild(firstname);
		    		
		    		Element lastname = doc.createElement("lastname");
		    		respondent.appendChild(lastname);
		    		
		    		Element respondentid = doc.createElement("respondentid");
		    		respondent.appendChild(respondentid);
	    		}
	         	
	    		Element notedate = doc.createElement("notedate");	    		
	    		DateFormat dateFormat = new SimpleDateFormat("ddMMyyyyHHmmss");    			    			
    			notedate.appendChild(doc.createTextNode(dateFormat.format(new Date())));
	    		root.appendChild(notedate);
	    		
	    		Element notetaker = doc.createElement("notetaker");
	    		root.appendChild(notetaker);
	    		
	    		Element videolocation = doc.createElement("videolocation");
	    		root.appendChild(videolocation);
	    		
	    		Element questions = doc.createElement("questions");
	    		root.appendChild(questions);
	    		
	    		Element notebody = doc.createElement("notebody");
	    		root.appendChild(notebody);
    		}
    		
    		// write the content into xml file
    		TransformerFactory transformerFactory = TransformerFactory.newInstance();
    		Transformer transformer = transformerFactory.newTransformer();
    		DOMSource source = new DOMSource(doc);
    		StreamResult result = new StreamResult(file);
     		transformer.transform(source, result);
     
    		System.out.println("New file created!");
     
    	  } catch (ParserConfigurationException | TransformerException e) {
    		e.printStackTrace();
    		return false;
    	  } 
    	return true;
    }
    
    public Boolean writeFile(File file)  {
    	if (!file.exists() && !createNewFile(file)) {    		
    		return false;    		
    	}
    	
    	try {
    		
    		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
    		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();    		
    		Document doc = docBuilder.parse(file);
    
    		Node firstname = doc.getElementsByTagName("firstname").item(0);
    		firstname.setTextContent(getFirstName());
    		
    		Node lastname = doc.getElementsByTagName("lastname").item(0);
    		lastname.setTextContent(getLastName());
    		
    		Node respondentid = doc.getElementsByTagName("respondentid").item(0);
    		respondentid.setTextContent(getRespondentId());
      
    		Node notetaker = doc.getElementsByTagName("notetaker").item(0);
    		notetaker.setTextContent(getNoteTaker());
    		
    		Node videolocation = doc.getElementsByTagName("videolocation").item(0);
    		videolocation.setTextContent(getVideoLocation());
    		
    		Node questions = doc.getElementsByTagName("questions").item(0);
    		if (getQuestions() != null)
    			questions.setTextContent(getQuestions());
    		
    		Node notebody = doc.getElementsByTagName("notebody").item(0);
    		if (getNoteBody() != null)
    			notebody.setTextContent(getNoteBody());
    		
    		// write the content into xml file
    		TransformerFactory transformerFactory = TransformerFactory.newInstance();
    		Transformer transformer = transformerFactory.newTransformer();
    		DOMSource source = new DOMSource(doc);
    		StreamResult result = new StreamResult(file);
    		transformer.transform(source, result);
     
    		//System.out.println("Done");
     
    	   } catch (ParserConfigurationException pce) {
    		pce.printStackTrace();
    	   } catch (TransformerException tfe) {
    		tfe.printStackTrace();
    	   } catch (IOException ioe) {
    		ioe.printStackTrace();
    	   } catch (SAXException sae) {
    		sae.printStackTrace();
    	   }
    	
    	return true;
	}

    public String getFirstName() {
        return firstName.get();
    }

    public void setFirstName(String firstName) {
        this.firstName.set(firstName);
    }

    public StringProperty firstNameProperty() {
        return firstName;
    }

    public String getLastName() {
        return lastName.get();
    }

    public void setLastName(String lastName) {
        this.lastName.set(lastName);
    }

    public StringProperty lastNameProperty() {
        return lastName;
    }

    public String getNoteTaker() {
        return noteTaker.get();
    }

    public void setNoteTaker(String noteTaker) {
        this.noteTaker.set(noteTaker);
    }

    public StringProperty noteTakerProperty() {
        return noteTaker;
    }

    public String getRespondentId() {
        return respondentId.get();
    }

    public void setRespondentId(String respondentId) {
        this.respondentId.set(respondentId);
    }

    public StringProperty respondentIdProperty() {
        return respondentId;
    }

    public String getNoteDate() {
        return noteDate.get();
    }

    public void setNoteDate(String noteDate) {
        this.noteDate.set(noteDate);
    }

    public StringProperty noteDateProperty() {
        return noteDate;
    }
    
    public String getNoteBody() {
        return noteBody.get();
    }

    public void setNoteBody(String noteBody) {
        this.noteBody.set(noteBody);
    }

    public StringProperty noteBodyProperty() {
        return noteBody;
    }
    
    public String getVideoLocation() {
        return videoLocation.get();
    }

    public void setVideoLocation(String videoLocation) {
        this.videoLocation.set(videoLocation);
    }

    public StringProperty videoLocationProperty() {
        return videoLocation;
    }
    
    public String getQuestions() {
        return questions.get();
    }

    public void setQuestions(String questions) {
        this.questions.set(questions);
    }

    public StringProperty questionsProperty() {
        return questions;
    }  
}
