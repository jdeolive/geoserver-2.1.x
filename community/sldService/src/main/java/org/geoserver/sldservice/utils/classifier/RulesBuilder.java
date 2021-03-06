package org.geoserver.sldservice.utils.classifier;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.feature.FeatureCollection;
import org.geotools.filter.function.Classifier;
import org.geotools.filter.function.ExplicitClassifier;
import org.geotools.filter.function.RangedClassifier;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.styling.Rule;
import org.geotools.styling.StyleBuilder;
import org.geotools.styling.StyleFactory;
import org.geotools.styling.Symbolizer;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Function;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;

/**
 * Creates a List of Rule using different classification methods Sets up only
 * filter not Symbolyzer Available Classification: Quantile,Unique Interval &
 * Equal Interval
 * 
 * @author kappu
 * 
 */
public class RulesBuilder {

	private FilterFactory2 ff;
	private StyleFactory styleFactory;
	private StyleBuilder sb;

	public RulesBuilder() {
		ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
		styleFactory = CommonFactoryFinder.getStyleFactory(GeoTools.getDefaultHints());
		sb = new StyleBuilder();
	}

	/**
	 * Generate a List of rules using quantile classification Sets up only
	 * filter not symbolizer
	 * 
	 * @param features
	 * @param property
	 * @param classNumber
	 * @return
	 */
	public List<Rule> quantileClassification(FeatureCollection features,
			String property, int classNumber, boolean open) {

		FeatureType fType;
		Classifier groups = null;
		try {
			Function classify = ff.function("Quantile", ff.property(property),
					ff.literal(classNumber));
			groups = (Classifier) classify.evaluate(features);
			if (groups instanceof RangedClassifier)
			    if(open)
			        return openRangedRules((RangedClassifier) groups, property);
			    else
			        return closedRangedRules((RangedClassifier) groups, property);
			else if (groups instanceof ExplicitClassifier)
				return this.explicitRules((ExplicitClassifier) groups, property);

		} catch (Exception e) {
			//System.out.println("RulesBuilder: failed to build Quantile Classification");
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Generate a List of rules using equal interval classification Sets up only
	 * filter not symbolizer
	 * 
	 * @param features
	 * @param property
	 * @param classNumber
	 * @return
	 */
	public List<Rule> equalIntervalClassification(FeatureCollection features, String property, int classNumber, boolean open) {
		Classifier groups = null;
		try {
			Function classify = ff.function("EqualInterval", ff.property(property), ff.literal(classNumber));
			groups = (Classifier) classify.evaluate(features);
			//System.out.println(groups.getSize());
			if (groups instanceof RangedClassifier)
			    if(open)
			        return openRangedRules((RangedClassifier) groups, property);
			    else
			        return closedRangedRules((RangedClassifier) groups, property);
			else if (groups instanceof ExplicitClassifier)
				return this.explicitRules((ExplicitClassifier) groups, property);

		} catch (Exception e) {
			//System.out.println("RulesBuilder: failed to build EqualInterval Classification");
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Generate a List of rules using unique interval classification Sets up
	 * only filter not symbolizer
	 * 
	 * @param features
	 * @param property
	 * @return
	 */
	public List<Rule> uniqueIntervalClassification(FeatureCollection features,
			String property) {
		Classifier groups = null;
		int classNumber = features.size();
		try {
			Function classify = ff.function("UniqueInterval", ff.property(property), ff.literal(classNumber));
			groups = (Classifier) classify.evaluate(features);
			if (groups instanceof RangedClassifier)
				return this.closedRangedRules((RangedClassifier) groups, property);
			else if (groups instanceof ExplicitClassifier)
				return this.explicitRules((ExplicitClassifier) groups, property);

		} catch (Exception e) {
			//System.out.println("RulesBuilder: failed to build UniqueInterval Classification");
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Generate Polygon Symbolyzer for each rule in list
	 * Fill color is choose from rampcolor
	 * @param rules
	 * @param ramp
	 */
	public void polygonStyle(List<Rule> rules, ColorRamp fillRamp) {

		Iterator<Rule> it;
		Rule rule;
		Iterator<Color> colors;
		Color color;

		try {
			//adjust the colorRamp with the correct number of classes
			fillRamp.setNumClasses(rules.size());
			colors = fillRamp.getRamp().iterator();
			
			it = rules.iterator();
			while (it.hasNext() && colors.hasNext()) {
				color = colors.next();
				rule = it.next();
				rule.setSymbolizers(new Symbolizer[] { sb.createPolygonSymbolizer(sb.createStroke(Color.BLACK,1),sb.createFill(color)) });
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * Generate Line Symbolyzer for each rule in list
	 * Stroke color is choose from rampcolor
	 * @param rules
	 * @param ramp
	 */
	public void lineStyle(List<Rule> rules, ColorRamp fillRamp) {

		Iterator<Rule> it;
		Rule rule;
		Iterator<Color> colors;
		Color color;

		try {
			//adjust the colorRamp with the correct number of classes
			fillRamp.setNumClasses(rules.size());
			colors = fillRamp.getRamp().iterator();

			it = rules.iterator();
			while (it.hasNext() && colors.hasNext()) {
				color = colors.next();
				rule = it.next();
				rule.setSymbolizers(new Symbolizer[] { sb.createLineSymbolizer(color) });
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}
	
	public StyleFactory getStyleFactory() {
		return this.styleFactory;
	}
	
	/**
     * Generate Rules from Rangedclassifier groups
     * build a List of rules
     * @param groups
     * @param property
     * @return
     */
    private List<Rule> openRangedRules(RangedClassifier groups, String property) {

        Rule r;
        Filter f;
        List<Rule> list = new ArrayList();
        PropertyName att = ff.property(property);
        try {
            /* First class */
            r = styleFactory.createRule();
            if(groups.getMin(0).equals(groups.getMax(0))){
                f = CQL.toFilter(att + " =" + ff.literal(groups.getMax(0)));
                r.setFilter(f);
                r.setTitle( ff.literal(groups.getMax(0)).toString());
                list.add(r);
            }else{
                f = CQL.toFilter(att + " <=" + ff.literal(groups.getMax(0)));
                r.setFilter(f);
                r.setTitle(" <= " + ff.literal(groups.getMax(0)));
                list.add(r);
            }
            for (int i = 1; i < groups.getSize() - 1; i++) {
                r = styleFactory.createRule();
                if(groups.getMin(i).equals(groups.getMax(i))){
                    f = CQL.toFilter(att + "=" + ff.literal(groups.getMin(i)));
                    r.setTitle( ff.literal(groups.getMin(i)).toString());
                    r.setFilter(f);
                    list.add(r);
                }else{
                    f = CQL.toFilter(att + ">" + ff.literal(groups.getMin(i)) + " AND " + att + " <=" + ff.literal(groups.getMax(i)));
                    r.setTitle(" > " + ff.literal(groups.getMin(i)) + " AND <= " + ff.literal(groups.getMax(i)));
                    r.setFilter(f);
                    list.add(r);
                }
            }
            /* Last class */
            r = styleFactory.createRule();
            if(groups.getMin(groups.getSize() - 1).equals(groups.getMax(groups.getSize() - 1))){
                f = CQL.toFilter(att + "=" + ff.literal(groups.getMin(groups.getSize() - 1)));
                r.setFilter(f);
                r.setTitle( ff.literal(groups.getMin(groups.getSize() - 1)).toString());
                list.add(r);
            }else{
                f = CQL.toFilter(att + ">" + ff.literal(groups.getMin(groups.getSize() - 1)));
                r.setFilter(f);
                r.setTitle(" > " + ff.literal(groups.getMin(groups.getSize() - 1)));
                list.add(r);
            }
            return list;
        } catch (CQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

	/**
	 * Generate Rules from Rangedclassifier groups
	 * build a List of rules
	 * @param groups
	 * @param property
	 * @return
	 */
	private List<Rule> closedRangedRules(RangedClassifier groups, String property) {

		Rule r;
		Filter f;
		List<Rule> list = new ArrayList();
		PropertyName att = ff.property(property);
		try {
			/* First class */
			r = styleFactory.createRule();
			for (int i = 0; i < groups.getSize(); i++) {
				r = styleFactory.createRule();
				if(i > 0 && groups.getMax(i).equals(groups.getMax(i -1)))
				    continue;
				if(groups.getMin(i).equals(groups.getMax(i))){
					f = CQL.toFilter(att + "=" + ff.literal(groups.getMin(i)));
					r.setTitle( ff.literal(groups.getMin(i)).toString());
					r.setFilter(f);
					list.add(r);
				} else {
					f = CQL.toFilter(att + (i == 0 ? ">=" : ">") + ff.literal(groups.getMin(i)) + " AND " + att + " <=" + ff.literal(groups.getMax(i)));
					r.setTitle(" > " + ff.literal(groups.getMin(i)) + " AND <= " + ff.literal(groups.getMax(i)));
					r.setFilter(f);
					list.add(r);
				}
			}
			return list;
		} catch (CQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Generate Rules from Explicitclassifier groups
	 * build a List of rules
	 * @param groups
	 * @param property
	 * @return
	 */
	private List<Rule> explicitRules(ExplicitClassifier groups, String property) {

		Rule r;
		Filter f;
		List<Rule> list = new ArrayList();
		PropertyName att = ff.property(property);
		String szFilter = "";
		String szTitle = "";
		Literal val;

		try {
			for (int i = 0; i < groups.getSize(); i++) {
				r = styleFactory.createRule();
				Set ls = groups.getValues(i);
				Iterator it = ls.iterator();
				val = ff.literal(it.next());
				szFilter = att + "=\'" + val + "\'";
				szTitle = "" + val;

				while (it.hasNext()) {
					val = ff.literal(it.next());
					szFilter += " OR " + att + "=\'" + val + "\'";
					szTitle += " OR " + val;
				}
				f = CQL.toFilter(szFilter);
				r.setTitle(szTitle);
				r.setFilter(f);
				list.add(r);
			}
			return list;
		} catch (CQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;

	}
}