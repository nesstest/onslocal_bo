package models;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import play.data.validation.ValidationError;

public class Metadata {
	  private String dsname;
	  private Long dimdsid;
	  private String json;
	  
	public String getDsname() {
		return dsname;
	}
	public void setDsname(String dsname) {
		this.dsname = dsname;
	}
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
      
      Pattern pattern = Pattern.compile("^[a-zA-Z0-9]*$");            
     
      if (dimdsid == null) {
        errors.add(new ValidationError("dimdsid", "No id was given."));
      }


          if (dsname == null || dsname.length() == 0) {
              errors.add(new ValidationError("dsname", "No title was given."));
          }
         
      
      if(errors.size() > 0){
          return errors;
      }
      else{
        return null;
      }  
    }
	
   public boolean isAlphaNumeric(String s){
      String pattern= "^[a-zA-Z0-9]*$";
      if(s.matches(pattern)){
        return true;
      }
      return false;   
   }
}
