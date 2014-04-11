package org.goobs.truth

import scala.collection.JavaConversions._
import edu.smu.tspell.wordnet.WordNetDatabase
import edu.stanford.nlp.natlog.Monotonicity
import edu.stanford.nlp.Sentence

/**
 * Tests for various NatLog functionalities
 *
 * @author gabor
 */
class NatLogTest extends Test {

  val DEFAULT = Monotonicity.DEFAULT match {
    case Monotonicity.UP => Messages.Monotonicity.UP
    case Monotonicity.DOWN => Messages.Monotonicity.DOWN
    case Monotonicity.NON => Messages.Monotonicity.FLAT
  }

  describe("Natural Logic Weights") {
    Props.NATLOG_INDEXER_LAZY = false
    describe("when a hard assignment") {
      it ("should accept Wordnet monotone jumps") {
        NatLog.hardNatlogWeights.getCount(Learn.unigramUp(EdgeType.WORDNET_UP)) should be >= -1.0
        NatLog.hardNatlogWeights.getCount(Learn.unigramDown(EdgeType.WORDNET_DOWN)) should be >= -1.0
        NatLog.hardNatlogWeights.getCount(Learn.bigramUp(EdgeType.WORDNET_UP, EdgeType.WORDNET_UP)) should be >= -1.0
        NatLog.hardNatlogWeights.getCount(Learn.bigramDown(EdgeType.WORDNET_DOWN, EdgeType.WORDNET_DOWN)) should be >= -1.0
      }
      it ("should accept Freebase monotone jumps") {
        NatLog.hardNatlogWeights.getCount(Learn.unigramUp(EdgeType.FREEBASE_UP)) should be >= -1.0
        NatLog.hardNatlogWeights.getCount(Learn.unigramDown(EdgeType.FREEBASE_DOWN)) should be >= -1.0
        NatLog.hardNatlogWeights.getCount(Learn.bigramUp(EdgeType.FREEBASE_UP, EdgeType.FREEBASE_UP)) should be >= -1.0
        NatLog.hardNatlogWeights.getCount(Learn.bigramDown(EdgeType.FREEBASE_DOWN, EdgeType.FREEBASE_DOWN)) should be >= -1.0
      }
      it ("should accept hybrid monotone jumps") {
        NatLog.hardNatlogWeights.getCount(Learn.bigramUp(EdgeType.WORDNET_UP, EdgeType.FREEBASE_UP)) should be >= -1.0
        NatLog.hardNatlogWeights.getCount(Learn.bigramDown(EdgeType.FREEBASE_DOWN, EdgeType.WORDNET_DOWN)) should be >= -1.0
      }
      it ("should NOT accept mixed jumps") {
        NatLog.hardNatlogWeights.getCount(Learn.bigramUp(EdgeType.FREEBASE_UP, EdgeType.FREEBASE_DOWN)) should be < 0.0
        NatLog.hardNatlogWeights.getCount(Learn.bigramDown(EdgeType.FREEBASE_DOWN, EdgeType.FREEBASE_UP)) should be < 0.0
        NatLog.hardNatlogWeights.getCount(Learn.bigramUp(EdgeType.WORDNET_UP, EdgeType.FREEBASE_DOWN)) should be < 0.0
        NatLog.hardNatlogWeights.getCount(Learn.bigramDown(EdgeType.FREEBASE_DOWN, EdgeType.WORDNET_UP)) should be < 0.0
      }
    }
  }


  describe("Monotonicity Markings") {
    Props.NATLOG_INDEXER_LAZY = false
    import Messages.Monotonicity._
    it ("should mark 'all'") {
      NatLog.annotate("all cats", "have", "tails").getWordList.map( _.getMonotonicity ).toList should be (List(DOWN, DOWN, UP, UP))
      NatLog.annotate("every cat", "has", "a tail").getWordList.map( _.getMonotonicity ).toList should be (List(DOWN, DOWN, UP, UP, UP))
    }
    it ("should mark 'some'") {
      NatLog.annotate("some cats", "have", "tails").getWordList.map( _.getMonotonicity ).toList should be (List(UP, UP, UP, UP))
      NatLog.annotate("there are cats", "which have", "tails").getWordList.map( _.getMonotonicity ).toList should be (List(UP, UP, UP, UP, UP, UP))
      NatLog.annotate("there exist cats", "which have", "tails").getWordList.map( _.getMonotonicity ).toList should be (List(UP, UP, UP, UP, UP, UP))
    }
    it ("should mark 'few'") {
      NatLog.annotate("few cat", "have", "tails").getWordList.map( _.getMonotonicity ).toList should be (List(DOWN, DOWN, DOWN, DOWN))
    }
    it ("should mark 'most'") {
      NatLog.annotate("most cats", "have", "tails").getWordList.map( _.getMonotonicity ).toList should be (List(FLAT, FLAT, UP, UP))
    }
    it ("should mark 'no'") {
      NatLog.annotate("no cats", "have", "tails").getWordList.map(_.getMonotonicity).toList should be(List(DOWN, DOWN, DOWN, DOWN))
    }
    it ("should mark 'not'") {
      NatLog.annotate("cat", "do not have", "tails").getWordList.map( _.getMonotonicity ).toList should be (List(DEFAULT, DOWN, DOWN, DOWN, DOWN))
      NatLog.annotate("cat", "don't have", "tails").getWordList.map( _.getMonotonicity ).toList should be (List(DEFAULT, DOWN, DOWN, DOWN, DOWN))
    }
    it ("should work on 'Every job that involves a giant squid is dangerous'") {
      NatLog.annotate("every job that involves a giant squid is dangerous").head.getWordList.map( _.getMonotonicity ).toList should be (
        List(DOWN, DOWN, DOWN, DOWN, DOWN, DOWN, DOWN, UP))
    }
    it ("should work on 'Not every job that involves a giant squid is safe'") {
      new Sentence("not every job that involves a giant squid is safe").words.length should be (10)
      NatLog.annotate("not every job that involves a giant squid is safe").head.getWordList.map( _.getMonotonicity ).toList should be (
        List(DOWN, UP, UP, UP, UP, UP, UP, UP, DOWN))
    }
  }

  describe("Lesk") {
    Props.NATLOG_INDEXER_LAZY = false
    it ("should be perfect for exact string matches") {
      NatLog.lesk(WordNetDatabase.getFileInstance.getSynsets("cat")(0), "feline mammal usually having thick soft fur and no ability to roar: domestic cats; wildcats".split("""\s+""")) should be (225.0)
    }
    it ("should be reasonable for multi-word overlaps") {
      NatLog.lesk(WordNetDatabase.getFileInstance.getSynsets("cat")(5), "cat be tracked vehicle".split("""\s+""")) should be (4.0)
      NatLog.lesk(WordNetDatabase.getFileInstance.getSynsets("cat")(5), "cat be large tracked vehicle".split("""\s+""")) should be (9.0)
    }
  }

  describe("Word Senses") {
    Props.NATLOG_INDEXER_LAZY = false
    it ("should get default sense of 'cat'") {
      NatLog.annotate("the cat", "have", "tail").getWordList.map( _.getPos ).toList should be (List("?", "n", "v", "n"))
      NatLog.annotate("the cat", "have", "tail").getWordList.map( _.getSense ).toList should be (List(0, 1, 2, 1))
    }
    it ("should get vehicle senses of 'CAT' with enough evidence") {
      NatLog.annotate("the cat", "be", "large tracked vehicle").getWordList.map( _.getPos ).toList should be (List("?", "n", "v", "j", "n"))
      NatLog.annotate("the cat", "be", "large tracked vehicle").getWordList.map( _.getSense ).toList should be (List(0, 6, 2, 2, 1))
    }
    it ("should get right sense of 'tail'") {
      NatLog.annotate("some cat", "have", "tail").getWordList.map( _.getSense ).toList should be (List(1, 1, 2, 1))
      NatLog.annotate("some animal", "have", "tail").getWordList.map( _.getSense ).toList should be (List(1, 1, 2, 1))
    }
  }
}