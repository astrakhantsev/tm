package ru.ispras.modis.tm.documents

import gnu.trove.map.hash.{TIntObjectHashMap, TObjectIntHashMap}
import ru.ispras.modis.tm.attribute.{AttributeType, DefaultAttributeType}
import ru.ispras.modis.tm.utils.DefaultChecker

/**
 * Created with IntelliJ IDEA.
 * User: padre
 * Date: 21.03.14
 * Time: 17:57
 */
/**
 * hold mapping form words index to words (string) and word to it serial number
 * @param indexWordsMap attribute -> (index of word from it attribute -> word from it attribute)
 */
class Alphabet(private[documents] val indexWordsMap: Map[AttributeType, TIntObjectHashMap[String]],
               private[documents] val wordsIndexMap: Map[AttributeType, TObjectIntHashMap[String]]) extends DefaultChecker with Serializable {
    /**
     *
     * @param attribute words attribute
     * @param index index of words
     * @return words, corresponding to attribute and index
     */
    def apply(attribute: AttributeType, index: Int) = indexWordsMap(attribute).get(index)

    /**
     * apply with default attribute, it is useful if you use only one attribute. It even more useful if you do not know
     * what attribute is and what is a multilingual model (e.g. you wont to run a classical PLSA).
     * @param index index of words
     * @return words, corresponding to index
     */
    def apply(index: Int) = {
        checkDefault(indexWordsMap)
        indexWordsMap(DefaultAttributeType).get(index)
    }

    /**
     *
     * @return map attributeType -> number of unique words, corresponding to this attribute.
     *         Guarantee that words index < number of unique words
     */
    def numberOfWords(): Map[AttributeType, Int] = indexWordsMap.map { case (key, value) => (key, value.size)}

    /**
     *
     * @param attribute attribute type
     * @return number of words, corresponding to given attribute
     */
    def numberOfWords(attribute: AttributeType): Int = numberOfWords()(attribute)

    /**
     * check if alphabet contain given word
     * @param attribute attribute of word
     * @param word input word
     * @return true if word in alphabet, false otherwise
     */
    def contain(attribute: AttributeType, word: String): Boolean = wordsIndexMap(attribute).contains(word)

    def contain(word: String): Boolean = {
        checkDefault(indexWordsMap)
        contain(DefaultAttributeType, word)
    }

    def getIndex(attribute: AttributeType, word: String): Option[Int] = {
        if (contain(attribute, word)) Some(wordsIndexMap(attribute).get(word)) else None
    }

    def getIndex(word: String): Option[Int] = {
        checkDefault(indexWordsMap)
        getIndex(DefaultAttributeType, word: String)
    }

    def getAttributes() = indexWordsMap.keySet
}

/**
 * companion object to construct Alphabet from wordIndexMap
 */
object Alphabet {
    /**
     *
     * @param wordIndexMap map from attribute to map from word to serial number of word
     * @return instance of class Alphabet
     */
    def apply(wordIndexMap: Map[AttributeType, TObjectIntHashMap[String]]) = {
        val indexWordMap = wordIndexMap.map { case (attribute, map) =>
            val reverseMap = new TIntObjectHashMap[String]()
            map.keys().foreach(key => reverseMap.put(map.get(key), key.toString))
            (attribute, reverseMap)
        }
        new Alphabet(indexWordMap, wordIndexMap)
    }
}