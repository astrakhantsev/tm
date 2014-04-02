package scripts

import documents.{TextualDocument, Numerator}
import plsa.RobustPLSAFactory
import initialapproximationgenerator.{GibbsInitialApproximationGenerator, RandomInitialApproximationGenerator}
import regularizer.ZeroRegularizer
import sparsifier.ZeroSparsifier
import stoppingcriteria.MaxNumberOfIterationStoppingCriteria
import java.util.Random
import brick.NoiseParameters
import scala.io.Source
import java.io.File
import attribute.Category

/**
 * Created with IntelliJ IDEA.
 * User: padre
 * Date: 28.03.14
 * Time: 13:49
 */
object PigData extends App {
    def getDocs(path: String) = {
        val lines = Source.fromFile(new File(path)).getLines().take(100000).map(line => new TextualDocument(Map(Category -> line.split(" "))))
        val random = new Random
        random.setSeed(13)
        val (docs, alphabet) = Numerator(lines.toSeq)
        val noiseParameter = new NoiseParameters(0.00f, 0.00f)
        val plsa = RobustPLSAFactory(new RandomInitialApproximationGenerator(random),
            new ZeroRegularizer(),
            docs,
            10,
            new ZeroSparsifier(),
            new ZeroSparsifier(),
            new MaxNumberOfIterationStoppingCriteria(100), //33
            alphabet,
            noiseParameter)
        (plsa, docs)
    }


    val readDataTime = System.currentTimeMillis()
    val (plsa, docs) = getDocs("/media/3d6a5a46-cbd3-49cd-abd4-2907eed0831a/home/padre/data/PigData/news")
    println(" readDataTime " + (System.currentTimeMillis() * 0.001 - readDataTime / 1000))
    val start = System.currentTimeMillis()
    val trainedModel = plsa.train(docs)
    println(trainedModel)
    println(" time " + (System.currentTimeMillis() * 0.001 - start / 1000))
}
