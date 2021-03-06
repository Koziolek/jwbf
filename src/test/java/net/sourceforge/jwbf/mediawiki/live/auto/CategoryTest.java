/*
 * Copyright 2007 Thomas Stock.
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
 * 
 */
package net.sourceforge.jwbf.mediawiki.live.auto;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.Iterator;

import lombok.extern.slf4j.Slf4j;
import net.sourceforge.jwbf.core.contentRep.SimpleArticle;
import net.sourceforge.jwbf.mediawiki.VersionTestClassVerifier;
import net.sourceforge.jwbf.mediawiki.actions.MediaWiki.Version;
import net.sourceforge.jwbf.mediawiki.actions.queries.CategoryMembersFull;
import net.sourceforge.jwbf.mediawiki.actions.queries.CategoryMembersSimple;
import net.sourceforge.jwbf.mediawiki.bots.MediaWikiBot;
import net.sourceforge.jwbf.mediawiki.contentRep.CategoryItem;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Verifier;
import org.junit.runners.Parameterized.Parameters;

import com.google.common.collect.Lists;

@Slf4j
public class CategoryTest extends ParamHelper {

  @ClassRule
  public static VersionTestClassVerifier classVerifier = new VersionTestClassVerifier(
      CategoryMembersFull.class, CategoryMembersSimple.class);

  @Rule
  public Verifier successRegister = classVerifier.getSuccessRegister(this);

  @Parameters(name = "{0}")
  public static Collection<?> stableWikis() {
    return ParamHelper.prepare(Version.valuesStable());
  }

  public CategoryTest(Version v) {
    super(v, classVerifier);
  }

  private static final int COUNT = 60;
  private static final String TESTCATNAME = "TestCat";

  protected void doPreapare(MediaWikiBot bot) {
    try {
      SimpleArticle a = new SimpleArticle();

      for (int i = 0; i < COUNT; i++) {
        a.setTitle("CategoryTest" + i);
        a.setText("abc [[Category:" + TESTCATNAME + "]]");
        bot.writeContent(a);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Test category read. Test category must have more then 50 members.
   * 
   */
  @Test
  public void doTest() {
    doTest(TESTCATNAME);
  }

  private void doTest(String catname) {

    Collection<String> compare1 = Lists.newArrayList();
    Collection<CategoryItem> compare2 = Lists.newArrayList();
    Iterator<String> is = new CategoryMembersSimple(bot, catname).iterator();
    int i = 0;
    boolean notEnough = true;
    while (is.hasNext()) {
      is.next();
      i++;
      if (i > 55) {
        notEnough = false;
        break;
      }
    }
    if (notEnough) {
      log.info("begin prepare");
      doPreapare(bot);
    }

    is = new CategoryMembersSimple(bot, catname).iterator();
    i = 0;
    while (is.hasNext()) {
      String x = is.next();
      if (!compare1.contains(x)) {
        compare1.add(x);
      } else {
        fail(x + " alredy in collection");
      }

      i++;
      if (i > 55) {
        break;
      }
    }
    assertTrue("i is: " + i, i > 50);

    Iterator<CategoryItem> cit = new CategoryMembersFull(bot, catname).iterator();
    i = 0;
    while (cit.hasNext()) {
      CategoryItem x = cit.next();
      if (!compare2.contains(x)) {
        compare2.add(x);
      } else {
        fail(x.getTitle() + " alredy in collection");
      }
      i++;
      if (i > 55) {
        break;
      }
    }
    assertTrue("i is: " + i, i > 50);

  }

}
