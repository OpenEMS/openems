package io.openems.edge.core.appmanager;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Base64;

import javax.imageio.ImageIO;

import io.openems.edge.app.pvinverter.FroniusPvInverter;

/**
 * Simple util class for edit images.
 */
public class ImageBuilder {

	private BufferedImage image;

	private ImageBuilder(BufferedImage image) {
		this.image = image;
	}

	/**
	 * Gets an ImageBuilder from an URL.
	 * 
	 * @param url the url of the image
	 * @return the {@link ImageBuilder}
	 */
	public static ImageBuilder of(URL url) {
		var buffImg = new BufferedImage(240, 240, BufferedImage.TYPE_INT_RGB);

		try {
			buffImg = ImageIO.read(url);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return new ImageBuilder(buffImg);
	}

	/**
	 * Gets an ImageBuilder from the path.
	 * 
	 * @param path the path of the image
	 * @return the {@link ImageBuilder}
	 */
	public static ImageBuilder of(String path) {
		File img = new File(path);

		var buffImg = new BufferedImage(240, 240, BufferedImage.TYPE_INT_ARGB);

		try {
			buffImg = ImageIO.read(img);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return new ImageBuilder(buffImg);
	}

	/**
	 * Resizes the image to the target values.
	 * 
	 * @param targetWidth  the target width
	 * @param targetHeight the target height
	 * @return this
	 */
	public ImageBuilder resize(int targetWidth, int targetHeight) {
		if (this.image == null) {
			return this;
		}
		BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics2D = resizedImage.createGraphics();
		graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics2D.drawImage(this.image, 0, 0, targetWidth, targetHeight, null);
		graphics2D.dispose();
		this.image = resizedImage;
		return this;
	}

	/**
	 * Applies the giving padding to the left and right side of the image.
	 * 
	 * <p>
	 * The height of the image will be set to the new width.
	 * 
	 * @param sidePadding the padding
	 * @return this
	 */
	public ImageBuilder applyPaddingLeftRight(int sidePadding) {
		if (this.image == null) {
			return this;
		}
		var width = this.image.getWidth() + (sidePadding * 2);
		var height = width;
		BufferedImage resizedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics2D = resizedImage.createGraphics();
		graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		var x = width / 2 - this.image.getWidth() / 2;
		var y = height / 2 - this.image.getHeight() / 2;
		graphics2D.drawImage(this.image, x, y, this.image.getWidth(), this.image.getHeight(), null);
		graphics2D.dispose();
		this.image = resizedImage;
		return this;
	}

	/**
	 * Resizes the width and the height so it matches its old dimensions.
	 * 
	 * @param targetWidth the target width
	 * @return this
	 */
	public ImageBuilder resizeWidth(int targetWidth) {
		return this.resize(targetWidth, this.getHeightOf(targetWidth));
	}

	private int getHeightOf(int targetWidth) {
		var scale = ((double) targetWidth / (double) this.image.getWidth());
		var height = (int) (this.image.getHeight() * scale);
		return height;
	}

	/**
	 * Scales the image for the given factor.
	 * 
	 * @param scaleFactor the scale factor
	 * @return this
	 */
	public ImageBuilder scale(double scaleFactor) {
		return this.resize((int) (this.image.getWidth() * scaleFactor), (int) (this.image.getHeight() * scaleFactor));
	}

	/**
	 * Gets the Image as a base64 String.
	 * 
	 * @return the String
	 */
	public String toBase64() {
		if (this.image == null) {
			return null;
		}
		final var prefix = "data:image/png;base64,";
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		try {
			ImageIO.write(this.image, "png", os);
			byte[] bytes = os.toByteArray();
			return prefix + Base64.getEncoder().encodeToString(bytes);
		} catch (IOException e) {
			return null;
		}
	}

	/**
	 * Saves the image to the given path.
	 * 
	 * @param path the path of the created image
	 * @return the created file
	 */
	public File saveTo(String path) {
		File outputfile = new File(path);
		try {
			ImageIO.write(this.image, "png", outputfile);
			return outputfile;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Main.
	 * 
	 * @param args the args
	 */
	public static void main(String[] args) {
		// default settings for fenecon images
		ImageBuilder.of(FroniusPvInverter.class.getResource("FEMS App_22_Fronius Wechselrichter_DE.png")) //
				.resizeWidth(400) //
				.applyPaddingLeftRight(200) //
				.saveTo("Fronius.png");
	}

}
