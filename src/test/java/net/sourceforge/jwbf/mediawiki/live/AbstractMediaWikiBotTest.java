package net.sourceforge.jwbf.mediawiki.live;

import static net.sourceforge.jwbf.TestHelper.assumeReachable;

import javax.inject.Provider;

import net.sourceforge.jwbf.mediawiki.bots.MediaWikiBot;

public abstract class AbstractMediaWikiBotTest implements Provider<MediaWikiBot> {

  private static final String WIKIPEDIA_DE = "http://de.wikipedia.org/w/index.php";

  protected MediaWikiBot bot = null;

  public MediaWikiBot get() {
    return bot;
  }

  public static String getWikipediaDeUrl() {
    assumeReachable(WIKIPEDIA_DE);
    return WIKIPEDIA_DE;
  }

}
