/*
 * Copyright 2007 Justus Bisser.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 * Contributors:
 * Thomas Stock
 */
package net.sourceforge.jwbf.mediawiki.actions.editing;

import java.util.Deque;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;
import net.sourceforge.jwbf.core.RequestBuilder;
import net.sourceforge.jwbf.core.actions.Get;
import net.sourceforge.jwbf.core.actions.Post;
import net.sourceforge.jwbf.core.actions.util.ActionException;
import net.sourceforge.jwbf.core.actions.util.HttpAction;
import net.sourceforge.jwbf.mediawiki.ApiRequestBuilder;
import net.sourceforge.jwbf.mediawiki.actions.MediaWiki;
import net.sourceforge.jwbf.mediawiki.actions.editing.GetApiToken.Intoken;
import net.sourceforge.jwbf.mediawiki.actions.util.MWAction;
import net.sourceforge.jwbf.mediawiki.bots.MediaWikiBot;
import net.sourceforge.jwbf.mediawiki.contentRep.SimpleFile;

import com.google.common.base.Strings;
import com.google.common.collect.Queues;

/**
 * <p>
 * To allow your bot to upload media in your MediaWiki. Add at least the following line to your
 * MediaWiki's LocalSettings.php:<br>
 * 
 * <pre>
 * $wgEnableUploads = true;
 * </pre>
 * 
 * For more details see also <a
 * href="http://www.mediawiki.org/wiki/Help:Configuration_settings#Uploads" >Upload Config</a>
 * 
 * @author Justus Bisser
 * @author Thomas Stock
 * 
 */
@Slf4j
public class FileUpload extends MWAction {

  private final SimpleFile a;
  private Deque<HttpAction> actions;
  private UploadAction actionHandler;

  public FileUpload(final SimpleFile simpleFile, MediaWikiBot bot) {
    super(bot.getVersion());
    if (!simpleFile.getFile().isFile() || !simpleFile.getFile().canRead()) {
      throw new ActionException("no such file " + simpleFile.getFile());
    }

    if (!bot.isLoggedIn()) {
      throw new ActionException("Please login first");
    }

    a = simpleFile;
    if (!simpleFile.getFile().exists()) {
      throw new IllegalStateException("file not found" + simpleFile.getFile());
    }
    switch (bot.getVersion()) {
    case MW1_15:
    case MW1_16:
      actionHandler = new NoApiUpload(simpleFile);
      break;

    default:

      actionHandler = new ApiUpload(bot, simpleFile);
      break;
    }
    actions = actionHandler.getActions();

  }

  public FileUpload(MediaWikiBot bot, String filename) {
    this(new SimpleFile(filename), bot);
  }

  /**
   * {@inheritDoc}
   */
  public HttpAction getNextMessage() {
    return actions.pop();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasMoreMessages() {
    return !actions.isEmpty();
  }

  @Override
  public String processReturningText(String xml, HttpAction hm) {
    return actionHandler.handleResponse(xml, hm);
  }

  private static class NoApiUpload implements UploadAction {

    private final SimpleFile a;

    public NoApiUpload(SimpleFile simpleFile) {
      a = simpleFile;
    }

    public Deque<HttpAction> getActions() {
      Deque<HttpAction> actions = Queues.newArrayDeque();
      Get g = new RequestBuilder(MediaWiki.URL_INDEX) //
          .param("title", MediaWiki.encode(a.getTitle())) //
          .param("action", "edit") //
          .buildGet();
      actions.add(g);

      log.info("WRITE: " + a.getTitle());
      Post post = new RequestBuilder(MediaWiki.URL_INDEX) //
          .param("title", "Special:Upload") //
          .buildPost();

      post.addParam("wpDestFile", a.getTitle());
      post.addParam("wpIgnoreWarning", "true");
      post.addParam("wpSourceType", "file");
      post.addParam("wpUpload", "Upload file");
      post.addParam("wpUploadFile", a.getFile());
      if (!Strings.isNullOrEmpty(a.getText())) {
        post.addParam("wpUploadDescription", a.getText());
      }

      actions.add(post);
      return actions;
    }

    public String handleResponse(String xml, HttpAction hm) {
      if (xml.contains("error")) {
        Pattern errFinder = Pattern.compile("<p>(.*?)</p>", Pattern.DOTALL | Pattern.MULTILINE);
        Matcher m = errFinder.matcher(xml);
        String lastP = "";
        while (m.find()) {
          lastP = MediaWiki.decode(m.group(1));
          log.error("Upload failed: " + lastP);
        }

        throw new ActionException("Upload failed - " + lastP);
      }
      return "";
    }

  }

  private static class ApiUpload implements UploadAction {
    private Deque<HttpAction> actions = Queues.newArrayDeque();
    private final MediaWikiBot bot;
    private final SimpleFile simpleFile;
    private GetApiToken getApiToken;

    public ApiUpload(MediaWikiBot bot, SimpleFile simpleFile) {
      this.bot = bot;
      this.simpleFile = simpleFile;
    }

    public Deque<HttpAction> getActions() {
      getApiToken = new GetApiToken(Intoken.EDIT, simpleFile.getFilename(), bot.getVersion(),
          bot.getUserinfo());
      actions.add(getApiToken.getNextMessage());
      return actions;
    }

    public String handleResponse(String xml, HttpAction hm) {
      if (getApiToken != null) {
        getApiToken.processReturningText(xml, hm);
        String token = getApiToken.getToken();
        getApiToken = null;
        Post upload = new ApiRequestBuilder() //
            .action("upload") //
            .formatXml() //
            .param("token", MediaWiki.encode(token)) //
            .param("filename", MediaWiki.encode(simpleFile.getTitle())) //
            .param("ignorewarnings", "true") //
            .buildPost();
        upload.addParam("file", simpleFile.getFile());
        actions.add(upload);
      }
      // file upload requires enabled uploads, upload rights and filesystem permisions
      return xml;
    }
  }

  private static interface UploadAction {

    Deque<HttpAction> getActions();

    String handleResponse(String xml, HttpAction hm);
  }

}
