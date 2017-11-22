/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sentry.provider.db.service.thrift;

import org.apache.sentry.core.common.utils.PubSub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

import static org.apache.commons.lang.StringEscapeUtils.escapeHtml;

/**
 * This servlet facilitates sending {topic, message } tuples to Servlet components 
 * subscribed to specific topics.
 * <p>
 * It uses publish-subscribe mechanism implemented by PubSub class.
 * The form generated by this servlet consists of the following elements:
 * <p>
 * a) Topic: pull-down menu of existing topics, i.e. the topics registered with
 * PubSub by calling PubSub.subscribe() API. This prevents entering invalid topic.
 * <p>
 * b) Message: text field for entering a message
 * <p>
 * c) Submit: button to submit (topic, message) tuple
 * <p>
 * d) Status: text area printing status of the request or help information.
 */
public class PubSubServlet extends HttpServlet {

  private static final Logger LOGGER = LoggerFactory.getLogger(PubSubServlet.class);

  private static final String FORM_GET =
    "<!DOCTYPE html>" +
    "<html>" +
    "<body>" +
    "<form>" +
    "<br><br><b>Topic:</b><br><br>" +
    "<select name='topic'/>%s</select>" +
    "<br><br><b>Message:</b><br><br>" +
    "<input type='text' size='50' name='message'/>" +
    "<br><br>" +
    "<input type='submit' value='Submit'/>" +
    "</form>" +
    "<br><br><b>Status:</b><br><br>" +
    "<textarea rows='4' cols='50'>%s</textarea>" +
    "</body>" +
    "</html>";

  /**
   * Return parameter on servlet request for the given name
   *
   * @param request: Servlet request
   * @param name: Name of parameter in servlet request
   * @return Parameter in servlet request for the given name, return null if can't find parameter.
   */
  private static String getParameter(ServletRequest request, String name) {
    String s = request.getParameter(name);
    if (s == null) {
      return null;
    }
    s = s.trim();
    return s.isEmpty() ? null : s;
  }

  /**
   * Parse the topic and message values and submit them via PubSub.submit() API.
   * Reject request for unknown topic, i.e. topic no one is subscribed to.
   */
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response)
          throws ServletException, IOException {
    String topic = getParameter(request, "topic");
    String message = getParameter(request, "message");
    response.setContentType("text/html;charset=utf-8");
    response.setStatus(HttpServletResponse.SC_OK);
    PrintWriter out = response.getWriter();

    String msg = "Topic is required, Message is optional.\nValid topics: " + PubSub.getInstance().getTopics();
    if (topic != null) {
      LOGGER.info("Submitting topic " + topic + ", message " + message);
      try {
        PubSub.getInstance().publish(PubSub.Topic.fromString(topic), message);
        msg = "Submitted topic " + topic + ", message " + message;
      } catch (Exception e) {
        msg = "Failed to submit topic " + topic + ", message " + message + " - " + e.getMessage();
        LOGGER.error(msg);
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
        return;
      }
    }

    StringBuilder topics = new StringBuilder();
    for (PubSub.Topic t : PubSub.getInstance().getTopics()) {
      topics.append("<option>").append(t.getName()).append("</option>");
    }

    String output = String.format(FORM_GET, topics.toString(), escapeHtml(msg));
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("HTML Page: " + output);
    }
    out.write(output);
    out.close();
    response.flushBuffer();
  }
}