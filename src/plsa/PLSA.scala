package plsa

import attribute.AttributeType
import brick.AbstractPLSABrick
import stoppingcriteria.StoppingCriteria
import sparsifier.Sparsifier
import matrix.{AttributedPhi, Theta}
import documents.Document
import regularizer.Regularizer
import grizzled.slf4j.Logging

/**
 * Created with IntelliJ IDEA.
 * User: padre
 * Date: 27.03.14
 * Time: 16:37
 */
class PLSA(private val bricks: Map[AttributeType, AbstractPLSABrick],
           private val stoppingCriteria: StoppingCriteria,
           private val thetaSparsifier: Sparsifier,
           private val regularizer: Regularizer,
           private val phi: Map[AttributeType, AttributedPhi],
           private val theta: Theta) extends Logging {

    def train(documents: Seq[Document]): TrainedModel = {
        val collectionLength = documents.foldLeft(0) {
            (sum, document) => sum + document.numberOfWords()
        }
        var numberOfIteration = 0
        var oldPpx = 0d
        var newPpx = 0d
        while (!stoppingCriteria(numberOfIteration, oldPpx, newPpx)) {
            oldPpx = newPpx
            newPpx = perplexity(bricks.foldLeft(0d) {
                case (sum, (attribute, brick)) =>
                    sum + brick.makeIteration(theta, phi(attribute), documents, numberOfIteration)
            }, collectionLength)
            info(newPpx)
            applyRegularizer()
            theta.dump()
            theta.sparsify(thetaSparsifier, numberOfIteration)
            numberOfIteration += 1
        }

        new TrainedModel(phi, theta)
    }

    def perplexity(logLikelihood: Double, collectionLength: Int) = math.exp(-logLikelihood / collectionLength)

    protected def makeIteration(iterationCnt: Int, ppx: Double, collectionLength: Int, documents: Seq[Document]) {

        val logLikelihood = bricks.foldLeft(0d) {case (sum, (attribute, brick)) =>
                sum + brick.makeIteration(theta, phi(attribute), documents, iterationCnt)
        }
        val newPpx = perplexity(logLikelihood, collectionLength)
        info(newPpx)
        applyRegularizer()
        theta.dump()
        theta.sparsify(thetaSparsifier, iterationCnt)

        if (!stoppingCriteria(iterationCnt, ppx, newPpx)) makeIteration(iterationCnt + 1, newPpx, collectionLength, documents)
    }

    private def applyRegularizer() {
        var t = 0
        var d = 0
        while (d < theta.numberOfRows) {
            while (t < theta.numberOfColumns) {
                theta.addToExpectation(d, t, regularizer.derivativeByTheta(d, t, theta, phi))
                t += 1
            }
            t = 0
            d += 1
        }
    }
}
