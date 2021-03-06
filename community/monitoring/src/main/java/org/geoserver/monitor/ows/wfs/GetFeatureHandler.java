package org.geoserver.monitor.ows.wfs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.geotools.xml.EMFUtils;

public class GetFeatureHandler extends WFSRequestObjectHandler {

    public GetFeatureHandler() {
        super("net.opengis.wfs.GetFeatureType");
    }

    @Override
    public List<String> getLayers(Object request) {
        List queries = (List) EMFUtils.get((EObject)request,"query");
        List<String> layers = new ArrayList();
        
        for (Object q : queries) {
            List typeNames = (List) EMFUtils.get((EObject) q, "typeName");
            
            for (Object o : typeNames) {
                layers.add(toString(o));
            }
             
        }
        return layers;
    }

}
