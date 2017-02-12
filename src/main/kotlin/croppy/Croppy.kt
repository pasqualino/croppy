package croppy

import java.awt.Graphics2D
import java.awt.GraphicsConfiguration
import java.awt.GraphicsEnvironment
import java.awt.RenderingHints.KEY_INTERPOLATION
import java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.io.File
import java.lang.Math.*
import java.util.*
import javax.imageio.ImageIO


class Croppy {
    private val STEP = 25
    private val LOG_2 = 0.6931471805599453

    fun thumb(image: BufferedImage, size: Int): BufferedImage {
        return when {
            image.width < image.height -> squareTallImage(resize(image, size, -1))
            image.width > image.height -> squareWideImage(resize(image, -1, size))
            else -> resize(image, size, size)
        }
    }

    private tailrec fun squareWideImage(image: BufferedImage): BufferedImage =
            if (image.height >= image.width) image
            else squareWideImage(cropWide(image, min(STEP, image.width - image.height)))


    private tailrec fun squareTallImage(image: BufferedImage): BufferedImage =
            if (image.width >= image.height) image
            else squareTallImage(cropTall(image, min(STEP, image.height - image.width)))

    private fun cropWide(image: BufferedImage, amount: Int): BufferedImage {
        println("Cropping $amount pixels")
        val left = image.getSubimage(0, 0, image.width - amount, image.height)
        val right = image.getSubimage(amount, 0, image.width - amount, image.height)

        return if (imageEntropy(right) > imageEntropy(left)) right else left
    }

    private fun cropTall(image: BufferedImage, amount: Int): BufferedImage {
        println("Cropping $amount pixels")
        val top = image.getSubimage(0, 0, image.width, image.height - amount)
        val bottom = image.getSubimage(0, amount, image.width, image.height - amount)

        return if (imageEntropy(top) > imageEntropy(bottom)) top else bottom
    }

    /**
     * Shannon entropy of an image
     */
    private fun imageEntropy(image: BufferedImage): Double {
        var n = 0
        val occ = HashMap<Int, Int>()
        for (i in 0..image.height - 1) {
            for (j in 0..image.width - 1) {
                val pixel = image.getRGB(j, i)

                val red = pixel shr 16 and 0xff
                val green = pixel shr 8 and 0xff
                val blue = pixel and 0xff
                val d = round(0.2989 * red + 0.5870 * green + 0.1140 * blue).toInt()

                if (occ.containsKey(d)) {
                    occ.put(d, occ[d]?.plus(1) as Int)
                } else {
                    occ.put(d, 1)
                }
                ++n
            }
        }
        var e = 0.0
        for ((_, value) in occ) {
            val p = value.toDouble() / n
            e += p * log2(p)
        }
        return -e
    }

    private fun log2(p: Double): Double = log(p) / LOG_2

    private fun resize(original: BufferedImage, destWidth: Int, destHeight: Int): BufferedImage {
        var width = destWidth
        var height = destHeight
        var xScale: Double = destWidth.toDouble() / original.width
        var yScale: Double = destHeight.toDouble() / original.height

        if (destWidth < 0) {
            xScale = yScale
            width = Math.rint(xScale * original.width).toInt()
        }

        if (destHeight < 0) {
            yScale = xScale
            height = Math.rint(yScale * original.height).toInt()
        }

        println("Resizing thumbnail: ${width}x$height px")

        val gc = getDefaultConfiguration()
        val result = gc.createCompatibleImage(width, height, original.colorModel.transparency)

        var g2d: Graphics2D? = null
        try {
            g2d = result.createGraphics()
            g2d.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BICUBIC)
            val at = AffineTransform.getScaleInstance(xScale, yScale)
            g2d.drawRenderedImage(original, at)
        } finally {
            g2d?.dispose()
        }

        return result
    }

    fun getDefaultConfiguration(): GraphicsConfiguration =
            GraphicsEnvironment
                    .getLocalGraphicsEnvironment()
                    .defaultScreenDevice
                    .defaultConfiguration
}

fun main(args: Array<String>) {
    val classLoader = Croppy::class.java.classLoader
    val file = File(classLoader.getResource("test/judging-llama.jpg").file)

    println("Reading input image: $file")
    val image: BufferedImage = ImageIO.read(file)

    println("Starting...")
    val croppy: Croppy = Croppy()
    val thumb = croppy.thumb(image, 256)

    val outFile = File("/tmp/out.jpg")
    println("Writing output image: $outFile")
    ImageIO.write(thumb, "jpg", outFile)
}
