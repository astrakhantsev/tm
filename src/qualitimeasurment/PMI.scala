package qualitimeasurment

import gnu.trove.map.TObjectFloatMap
import gnu.trove.map.hash.{TObjectFloatHashMap, TIntFloatHashMap}
import matrix.AttributedPhi
import documents.{Alphabet}
import scala.io.Source
import java.io.File
import attribute.AttributeType

/**
 * Created with IntelliJ IDEA.
 * User: padre
 * Date: 04.04.14
 * Time: 19:37
 */
/**
 * calculate pmi for given topics. pmi is correlated with topic coherence. See for example:
 * "Automatic Evaluation of Topic Coherence by David Newman et al."
 * @param unigrams map from word index to number of occurrence of word in train collection.
 * @param bigrams map from pair of word index to number of occurrence of pair in train collection.
 * @param n number of top n words to calculate pmi
 * @param attribute attribute type
 */
class PMI(private val unigrams: TIntFloatHashMap,
          private val bigrams: TObjectFloatMap[Bigram],
          private val n: Int,
          private val attribute: AttributeType) {

    /**
     *
     * @param word index of word
     * @return number of occurrence of words in trained collection + 1
     */
    private def getWordWeight(word: Int) = unigrams.get(word) + 1f

    /**
     *
     * @param word index of the first word in bigram
     * @param otherWord index of the second word in bigram
     * @return  number of occurrence of bigram in trained collection + 1
     */
    private def getBigramWeight(word: Int, otherWord: Int) = bigrams.get(Set(word, otherWord)) + 1f

    /**
     * calculate pmi for two words
     * @param word the first word
     * @param otherWord the second word
     * @return pmi for pair of words
     */
    private def pairPMI(word: Int, otherWord: Int) = math.log(getWordWeight(word) * getWordWeight(otherWord) / getBigramWeight(word, otherWord))

    /**
     * return top most frequent word for given topic
     * @param topic array of words weight in topic
     * @return array of words indexes
     */
    private def getTopWords(topic: Array[Float]) = topic.zipWithIndex.sortBy(-_._1).take(n).map(_._2)

    /**
     *
     * @param words calculate pmi for every pair of word in given array.
     * @return array of pmi for every pair of words
     */
    private def pmi(words: Array[Int]) = words.flatMap(word => words.filter(_ != word).map(otherWord => pairPMI(word, otherWord)))

    /**
     * calculate the arithmetic mean of given array
     * @param array array of double
     * @return arithmetic mean of array elements
     */
    private def mean(array: Array[Double]) = array.sum / array.length

    /**
     * calculate median for input array
     * http://en.wikipedia.org/wiki/Median
     * @param array array of double
     * @return median of array
     */
    private def median(array: Array[Double]) = {
        if(array.length % 2 == 0)
            (array.sorted.apply(array.length / 2) + array.sorted.apply(array.length / 2 + 1)) / 2
        else
            array.sorted.apply(array.length / 2)
    }

    /**
     * calculate averege pmi for given words. average may be mean or median
     * @param phi matrix of distribution of words by topic
     * @param average function to calculate a
     * @return average pmi for pairs of given words
     */
    private def averegePMI(phi: AttributedPhi, average:Array[Double] => Double) = {
        require(attribute == phi.attribute, "type of attribute in phi and in this does not correspond")
        (0 until phi.numberOfRows).map{ topicIndex =>
            val topWords = getTopWords((0 until phi.numberOfColumns).map(wordIndex => phi.probability(topicIndex, wordIndex)).toArray)
            (topicIndex, average(pmi(topWords)))
        }.toArray
    }

    /**
     *  calculate mean pmi for given words.
     * @param phi matrix of distribution of words by topic
     * @return mean pmi for pairs of given words
     */
    def meanPMI(phi: AttributedPhi) = averegePMI(phi, mean)

    /**
     *  calculate median pmi for given words.
     * @param phi matrix of distribution of words by topic
     * @return median pmi for pairs of given words
     */
    def medianPMI(phi: AttributedPhi) = averegePMI(phi, median)
}

/**
 * companion object construct PMI from files with weight of bigrams and unigrams
 */
object PMI {
    /**
     * build pmi from files with bigram and file with unigram. Order of words in bigram is not important, every unigram
     * and bigram should occure only once.
     * every string of file with unigram contain word and number of occurrence of word in train collection, separated  by sep
     * for example, if sep = ",":
     * my,100
     * shiny,200
     * ass,3000
     * analogously for file with bigrams (sep = ","):
     * bite,my,100
     * my,ass,200
     * shiny,ass,300
     * @param pathToUnigrams path to file with unigrams
     * @param pathToBigrams path to file with bigrams
     * @param alphabet alphabet to map word to index
     * @param n number of top words to take to calculate pmi
     * @param attribute attribute type
     * @param sep separator in bigrams and unigrams file
     * @return instance of class PMI
     */
    def apply(pathToUnigrams: String, pathToBigrams: String, alphabet: Alphabet, n: Int,  attribute: AttributeType, sep: String = ","): PMI = {
        val unigrams = loadUnigrams(pathToUnigrams: String, alphabet: Alphabet, attribute: AttributeType, sep)
        val bigrams  = loadBigrams(pathToBigrams: String, alphabet: Alphabet, attribute: AttributeType, sep)
        new PMI(unigrams, bigrams, n, attribute)
    }

    /**
     *
     * @param pathToUnigrams path to file with unigrams
     * @param alphabet alphabet to map word to index
     * @param attribute attribute type
     * @param sep separator in bigrams file
     * @return trove map with unigrams weight
     */
    private def loadUnigrams(pathToUnigrams: String, alphabet: Alphabet, attribute: AttributeType, sep: String) = {
        val map = new TIntFloatHashMap()
        Source.fromFile(new File(pathToUnigrams)).getLines().map(_.split(sep)).filterNot(_.isEmpty).foreach{ wordAndWeight =>
            val wordIndex = alphabet.getIndex(attribute, wordAndWeight(0))
            val weight = wordAndWeight(1).toFloat
            if(wordIndex.nonEmpty) map.put(wordIndex.get, weight)
        }
        map
    }

    /**
     *
     * @param pathToBigrams path to file with bigrams
     * @param alphabet alphabet to map word to index
     * @param attribute attribute type
     * @param sep separator in bigrams  file
     * @return trove map from bigram (two words, replaced by serial number and placed in set) to the number of occurrence in collection
     */
    private def loadBigrams(pathToBigrams: String, alphabet: Alphabet, attribute: AttributeType, sep: String) = {
        val map = new TObjectFloatHashMap[Bigram]()
        Source.fromFile(new File(pathToBigrams)).getLines().map(_.split(sep)).filterNot(_.isEmpty).foreach{ wordsAndWeight =>
            val wordIndex = alphabet.getIndex(attribute, wordsAndWeight(0))
            val otherWordIndex = alphabet.getIndex(attribute, wordsAndWeight(1))
            val weight = wordsAndWeight(2).toFloat
            if(wordIndex.nonEmpty && otherWordIndex.nonEmpty) map.put(new Bigram(wordIndex.get, otherWordIndex.get), weight)
        }
        map
    }
}


