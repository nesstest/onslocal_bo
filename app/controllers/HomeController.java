package controllers;

import javax.inject.Inject;

import models.Dataset;
import models.Editor;
import models.Metadata;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.create;
import views.html.log;



/**
 * This controller contains an action to handle HTTP requests
 * to the application's home page.
 */
public class HomeController extends Controller {

    /**
     * An action that renders an HTML page with a welcome message.
     * The configuration in the <code>routes</code> file means that
     * this method will be called when the application receives a
     * <code>GET</code> request with a path of <code>/</code>.
     */ 
	
	@Inject 
	FormFactory formFactory;
    
    public Result create() {
        return ok(create.render("create a dataset"));
   }

    public Result load() {
    	Form<Dataset> dsForm = formFactory.form(Dataset.class);
        return ok(views.html.load.render(dsForm));
   // 	return ok(load.render("Choose a dataset to load."));
    }
    
    public Result log() {
        return ok(log.render("log"));
    }    

    public Result metadata() {
    	Form<Metadata> metaForm = formFactory.form(Metadata.class);
    	Metadata m1 = new Metadata();
    	m1.setDsname("TEST1");
    	m1.setDimdsid(11L);
        return ok(views.html.metadata.render(metaForm.fill(m1)));
   // 	return ok(load.render("Choose a dataset to load."));
    }
    
    public Result edit() {
    	Form<Editor> editForm = formFactory.form(Editor.class);
    	Editor ed1 = new Editor();
    	ed1.setTask("T");
    	ed1.setDsname("TEST1");
    	ed1.setDimdsid(11L); 
        return ok(views.html.edit.render(editForm.fill(ed1)));
   // 	return ok(load.render("Choose a dataset to load."));
    }    
    
    public Result index() {
       return ok(views.html.index.render("Welcome"));
  }
}
