package models;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import play.data.validation.ValidationError;

public class Editor {
	  private String dsname;
	  private Long dimdsid;
	  private String task;
	  private String status;
	  private List<String> dsList; 
	  
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
	public String getTask() {
		return task;
	}
	public void setTask(String task) {
		this.task = task;
	}

   public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}

   public List<String> getDsList() {
		return dsList;
	}
	public void setDsList(List<String> dsList) {
		this.dsList = dsList;
	}
public List<ValidationError> validate() {

      List<ValidationError> errors = new ArrayList<ValidationError>();
      
      if (dsname == null) {
        errors.add(new ValidationError("dataset id", "No id was given."));
      }
      
      if(errors.size() > 0){
          return errors;
      }
      else{
        return null;
      }  
    }
	
}