package net.sourceforge.jwbf.mediawiki.live;

import static net.sourceforge.jwbf.TestHelper.getRandomAlpha;
import static net.sourceforge.jwbf.mediawiki.BotFactory.getMediaWikiBot;
import static org.junit.Assert.assertTrue;
import net.sourceforge.jwbf.core.bots.WikiBot;
import net.sourceforge.jwbf.core.bots.util.JwbfException;
import net.sourceforge.jwbf.core.contentRep.Article;
import net.sourceforge.jwbf.mediawiki.actions.MediaWiki.Version;
import net.sourceforge.jwbf.mediawiki.bots.MediaWikiBot;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class ThreadTest {

  private MediaWikiBot bot;

  private static final String TITLE = "ThreadTest";

  @Before
  public void before() {
    try {
      synchronized (this) {
        bot = getMediaWikiBot(Version.getLatest(), true);
        // bot.postDelete(TITLE);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testWithBots() throws JwbfException {

    for (int i = 0; i < 10; i++) {
      Worker w = new Worker(bot);
      w.start();
    }
    synchronized (this) {
      try {
        wait(500);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    assertTrue(bot.getArticle(TITLE).getText().trim().length() > 1);
  }

  private static class Worker extends Thread {

    private final WikiBot bot;

    public Worker(WikiBot bot) {
      this.bot = bot;
    }

    @Override
    public void run() {
      while (true) {
        Article a = new Article(bot, TITLE);
        a.addText(getRandomAlpha(1));
        a.save(getName());
      }
    }
  }
}
