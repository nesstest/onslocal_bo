package controllers;

import play.*;
import play.data.Form;
import play.mvc.*;
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
public class StatusController extends Controller {
	@Inject 
	FormFactory formFactory;
	
	@Inject
	JPAApi jpaApi;
    
    @Transactional
    public Result getDatasets(){
    	EntityManager em = jpaApi.em();
        List <DimensionalDataSet> gcet = em.createQuery("SELECT d FROM DimensionalDataSet d",DimensionalDataSet.class).getResultList();
        return ok(views.html.status.render(gcet));    
    }
    
}
