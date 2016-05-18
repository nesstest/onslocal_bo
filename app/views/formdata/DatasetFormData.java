package views.formdata;

import play.data.validation.ValidationError;

import java.util.ArrayList;
import java.util.List;

/**
 * Backing class for the Student data form.
 * Requirements:
 * <ul>
 * <li> All fields are public, 
 * <li> All fields are of type String or List[String].
 * <li> A public no-arg constructor.
 * <li> A validate() method that returns null or a List[ValidationError].
 * </ul>
 */
public class DatasetFormData {

  public String id = "";
  public String title = ""; 
  public String filename = "";
 

  /** Required for form instantiation. */
  public DatasetFormData() {
  }

  /**
   * Creates an initialized form instance. Assumes the passed data is valid. 
   * @param name The id.
   * @param password The title.
   * @param level The filename.   
   */
  public DatasetFormData(String id, String title, String filename) {
    this.id = id;
    this.title = title;
    this.filename = filename;   
  }

  /**
   * Validates Form<DatasetFormData>.
   * Called automatically in the controller by bindFromRequest().
   * 
   * Validation checks include:
   * <ul>
   * <li> Id must be non-empty.
   * <li> Title must be at least nnnn characters?????.
   * <li> Filename must be supplied   
   * </ul>
   *
   * @return Null if valid, or a List[ValidationError] if problems found.
   */
  public List<ValidationError> validate() {

    List<ValidationError> errors = new ArrayList<ValidationError>();

    if (id == null || id.length() == 0) {
      errors.add(new ValidationError("id", "No id was given."));
    }

    if (title == null || title.length() == 0) {
        errors.add(new ValidationError("title", "No title was given."));
    }
    
    if (filename == null || filename.length() == 0) {
        errors.add(new ValidationError("filename", "No filename was given."));
    }    

    if(errors.size() > 0)
      return errors;

    return null;
  }
}
