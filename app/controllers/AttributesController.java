package controllers;

import play.*;
import play.data.Form;
import play.mvc.*;
import play.twirl.api.Html;
import views.html.*;
import models.*;
import play.data.FormFactory;
import play.data.validation.ValidationError;

import java.util.ArrayList;
import java.util.List;

import javax.inject.*;
import javax.persistence.EntityManager;

import services.LoadToTarget;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;
/**
 * This controller contains an action to process the input file
 */
public class AttributesController extends Controller {
	@Inject 
	FormFactory formFactory;

	private String datasetid;
	
	@Inject
	JPAApi jpaApi;
	
	@Transactional
	public Result prepareAtts(String dsid) {
    	datasetid = dsid;
		EntityManager em = jpaApi.em();
		Form<Attribs> attForm = formFactory.form(Attribs.class);
    	Attribs a1 = new Attribs();
    	List <DataResource> dis = em.createQuery("SELECT d FROM DataResource d WHERE d.dataResource = :dsid",DataResource.class).setParameter("dsid", datasetid).getResultList();
    	DataResource drs = dis.get(0);
    	a1.setColumn_concept(drs.getColumnConcept());
    	a1.setRow_concept(drs.getRowConcept());
    	a1.setDataResource(datasetid);
    	ArrayList<String> conList = new ArrayList();
    	List <ConceptSystem> cons = em.createQuery("SELECT c FROM ConceptSystem c",ConceptSystem.class).getResultList();
    	conList.add("Area");
    	conList.add("Time");
    	for (int i=0; i< cons.size(); i++){
    		conList.add(cons.get(i).getConceptSystem());
    	}
    	a1.setConceptList(conList);
    	return ok(views.html.attribs.render(attForm.fill(a1),conList)); 
	}
	
	@Transactional
    public Result processform() {
    	EntityManager em = jpaApi.em();
		Form<Attribs> attForm = formFactory.form(Attribs.class);
    	Attribs a1 = attForm.bindFromRequest().get();
    	List <DataResource> dis = em.createQuery("SELECT d FROM DataResource d WHERE d.dataResource = :dsid",DataResource.class).setParameter("dsid", datasetid).getResultList();
    	DataResource drs = dis.get(0);
    	drs.setRowConcept(a1.getRow_concept());
		drs.setColumnConcept(a1.getColumn_concept());
		em.persist(drs);
		return ok(views.html.message.render(("Attributes updated"), Html.apply("<p>Column Concept: " + a1.getColumn_concept() + "<br/>Row Concept: " + a1.getRow_concept() + "</p>")));

    }
    
}
