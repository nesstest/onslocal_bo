package models;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import play.data.validation.ValidationError;

public class Metadata {
	  private Long dimdsid;
	  private String json;
	    private String status;
	  
	public Long getDimdsid() {
		return dimdsid;
	}
	public void setDimdsid(Long dimdsid) {
		this.dimdsid = dimdsid;
	}
	public String getJson() {
		return json;
	}
	public void setJson(String json) {
		this.json = json;
	}
	
   public List<ValidationError> validate() {

      List<ValidationError> errors = new ArrayList<ValidationError>();
      
      if (dimdsid == null) {
        errors.add(new ValidationError("dimdsid", "No id was given."));
      }
      
      if (json == null || json.length() == 0) {
         errors.add(new ValidationError("json", "No JSON was given."));
       }
      
      if(errors.size() > 0){
          return errors;
      }
      else{
        return null;
      }  
    }
	
}
