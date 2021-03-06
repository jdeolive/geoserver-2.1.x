package org.geoserver.web.wps;

import java.util.List;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.wps.web.WPSRequestBuilder;

public class WPSRequestBuilderTest extends GeoServerWicketTestSupport {

    public void testJTSAreaWorkflow() throws Exception {
        login();

        // start the page
        tester.startPage(new WPSRequestBuilder());

        tester.assertComponent("form:requestBuilder:process", DropDownChoice.class);

        // look for JTS area
        DropDownChoice choice = (DropDownChoice) tester
                .getComponentFromLastRenderedPage("form:requestBuilder:process");
        int index = -1;
        final List choices = choice.getChoices();
        for (int i = 0; i < choices.size(); i++) {
            if (choices.get(i).equals("JTS:area")) {
                index = 0;
                break;
            }
        }

        // choose a process
        FormTester form = tester.newFormTester("form");
        form.select("requestBuilder:process", index);
        tester.executeAjaxEvent("form:requestBuilder:process", "onchange");

        // print(tester.getComponentFromLastRenderedPage("form"), true, true);

        // check process description
        tester.assertModelValue("form:requestBuilder:process", "JTS:area");
        Label label = (Label) tester
                .getComponentFromLastRenderedPage("form:requestBuilder:descriptionContainer:processDescription");
        assertTrue(label.getDefaultModelObjectAsString().contains("geometry area"));

        tester.assertComponent(
                "form:requestBuilder:inputContainer:inputs:0:paramValue:editor:mime",
                DropDownChoice.class);
        tester.assertComponent(
                "form:requestBuilder:inputContainer:inputs:0:paramValue:editor:textarea",
                TextArea.class);

        // fill in the params
        form = tester.newFormTester("form");
        form.select("requestBuilder:inputContainer:inputs:0:paramValue:editor:mime", 2);
        form.setValue("requestBuilder:inputContainer:inputs:0:paramValue:editor:textarea",
                "POLYGON((0 0, 0 10, 10 10, 10 0, 0 0))");
        form.submit();
        tester.clickLink("form:execute", true);

        // print(tester.getLastRenderedPage(), true, true);

        assertTrue(tester.getComponentFromLastRenderedPage("responseWindow")
                .getDefaultModelObjectAsString().contains("wps:Execute"));

        // unfortunately the wicket tester does not allow us to get work with the popup window
        // contents,
        // as that requires a true browser to execute the request
    }
}
