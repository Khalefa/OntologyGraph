package eg.edu.alexu.ehr.ontology.api.wrapper.object.entities;

import java.net.URI;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLEntity;

import eg.edu.alexu.ehr.ontology.api.wrapper.object.OntologyObject;

abstract public class OntologyEntity extends OntologyObject { // Class or datatype
	protected OWLEntity entity;
	protected boolean isClass;
	public OntologyEntity(OWLEntity object) {
		entity = object;
		isEntity = true;
	}
	
	public boolean isClass() {
		return isClass;
	}
	
	public boolean isDatatype() {
		return !isClass;
	}
	
	public IRI getIRI() {
		return ((OWLEntity)entity).getIRI();
	}
	
	public URI getURI() {
		return ((OWLEntity)entity).getIRI().toURI();
	}
	
	public String getURIAsStr() {
		return ((OWLEntity)entity).getIRI().toString();
	}
	
	@Override
	public int hashCode() {
		return entity.getIRI().toString().hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		return entity.getIRI().toString().equals(((OntologyEntity)obj).entity.getIRI().toString());
	}
	
	@Override
	public String toString() {
		return entity.getIRI().toString();
	}
}
