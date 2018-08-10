import utility.Checksum;
import utility.FastRGB;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Scanner;

public class Main {
	public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
		Scanner scan = new Scanner(System.in);

		String s = "";

		while (!s.equals("e") && !s.equals("d")) {
			System.out.println("Encode (e) or Decode (d)?");
			s = scan.nextLine();
		}

		if (s.equals("e")) {
			File file = getFile(scan);

			File outputFile = null;

			System.out.println("Output File: ");
			String fileStr = scan.nextLine();
			outputFile = new File(fileStr);

			encode(file, outputFile);
		} else if (s.equals("d")) {

			File file = getFile(scan);

			File outputFile = null;

			System.out.println("Output File: ");
			String fileStr = scan.nextLine();
			outputFile = new File(fileStr);

			decode(ImageIO.read(file), outputFile);
		} else {
			System.out.println("Something Broke");
		}

	}

	private static File getFile(Scanner scan) {
		String fileStr;
		boolean isFileValid = false;
		File file = null;

		while (!isFileValid) {
			System.out.println("Input file: ");
			fileStr = scan.nextLine();
			file = new File(fileStr);

			isFileValid = file.exists();
		}
		return file;
	}

	public static void decode(BufferedImage image, File outputFile) throws IOException, NoSuchAlgorithmException {
		long startTime = System.nanoTime();
		FastRGB fastRGB = new FastRGB(image);

		int size = image.getHeight() * image.getWidth();
		byte[] data = new byte[size * 3];

		int dataNum = 0;

		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				byte[] pixel = fastRGB.getRGB(x, y);

				data[dataNum] = pixel[0];
				data[dataNum + 1] = pixel[1];
				data[dataNum + 2] = pixel[2];

				dataNum += 3;
			}
		}

		byte[] checksum;

		int firstIndex = 0;
		int lastIndex = 0;


		int currentIndex = data.length-1;
		while (lastIndex==0) {
			if (data[currentIndex--]!=0) {
				lastIndex = currentIndex;
				firstIndex = currentIndex-19;
			}
		}

		checksum = Arrays.copyOfRange(data, firstIndex+1, firstIndex+21);

		byte[] newData = new byte[firstIndex+1];

		for (int i=0; i<firstIndex+1; i++) {
			newData[i] = data[i];
		}

		// File should be encoded in data


		FileOutputStream fos = new FileOutputStream(outputFile);

		fos.write(newData);
		fos.close();

		byte[] newChecksum = Checksum.getFileChecksum(outputFile);

		if (Arrays.equals(checksum, newChecksum)) {

			System.out.println("Checksum Valid");
			
		} else {
			System.out.println("Checksum invalid");

			System.out.println("Old Checksum: ");
			for (byte b : checksum) {
				System.out.print(b + " ");
			}

			System.out.println("\nNew Checksum");
			for (byte b : newChecksum) {
				System.out.print(b + " ");
			}
			System.out.println();
		}

		long endTime = System.nanoTime();

		double deltaTime = (double) (endTime - startTime) / 1000000000.0;

		System.out.println("Decoded in: " + deltaTime + " seconds");
	}

	public static void encode(File file, File outputFile) throws IOException, NoSuchAlgorithmException {
		long startTime = System.nanoTime();
		if (!file.exists()) {
			System.out.println("File does not exist!");
			System.exit(1);
		}

		FileInputStream fis = new FileInputStream(file);

		long size = file.length();

		byte[] checksum = Checksum.getFileChecksum(file);

		size += checksum.length;

		int closestSquare = (int) Math.ceil(Math.sqrt(Long.divideUnsigned(size, 3L)));

		ImageCreator ic = new ImageCreator(closestSquare, closestSquare);

		int pixleNum = 0;

		byte[] byteArray = new byte[4096];
		byte[] tempStorage = new byte[0];
		int bytesCount = 0;

		while ((bytesCount = fis.read(byteArray)) != -1) {
			for (int i = 0; i < bytesCount; i += 3) {
				int row = (pixleNum - (pixleNum % closestSquare)) / closestSquare;
				int col = (pixleNum - closestSquare * row);

				try {

					if (tempStorage.length == 1) {
						ic.drawPixel(row, col, new Color(tempStorage[0] & 0xFF,
								byteArray[i] & 0xFF, byteArray[i + 1] & 0xFF));
						i--;
						tempStorage = new byte[0];
					} else if (tempStorage.length == 2) {
						ic.drawPixel(row, col, new Color(tempStorage[0] & 0xFF,
								tempStorage[1] & 0xFF, byteArray[i] & 0xFF));
						i-=2;
						tempStorage = new byte[0];
					} else {

						ic.drawPixel(row, col, new Color(byteArray[i] & 0xFF,
								byteArray[i + 1] & 0xFF, byteArray[i + 2] & 0xFF));
					}

				} catch (ArrayIndexOutOfBoundsException e) {

					if (tempStorage.length != 0) {
						System.out.println("Something broke");
					}

					tempStorage = Arrays.copyOfRange(byteArray, i, byteArray.length); // Copy up to the last two bytes
					pixleNum--; // Counteract the ++

					/*try {
						ic.drawPixel(row, col, new Color(byteArray[i] & 0xFF,
								byteArray[i + 1] & 0xFF, 0));
					} catch (ArrayIndexOutOfBoundsException ee) {
						try {
							ic.drawPixel(row, col, new Color(byteArray[i] & 0xFF, 0, 0));
						} catch (ArrayIndexOutOfBoundsException eee) {

						}
					}*/
				}
				pixleNum++;
			}
		}

        for (int i=0; i<checksum.length; i+=3) {
	        int row = (pixleNum - (pixleNum % closestSquare)) / closestSquare;
	        int col = (pixleNum - closestSquare * row);

            try {
                ic.drawPixel(row, col, new Color(checksum[i] & 0xFF, checksum[i + 1] & 0xFF, checksum[i + 2] & 0xFF));
            } catch (ArrayIndexOutOfBoundsException e) {
                try {
                    ic.drawPixel(row, col, new Color(checksum[i] & 0xFF, checksum[i + 1] & 0xFF, 0));
                } catch (ArrayIndexOutOfBoundsException ee) {
                	try {
						ic.drawPixel(row, col, new Color(checksum[i] & 0xFF, 0, 0));
					} catch (ArrayIndexOutOfBoundsException eee) {

					}
                }
            }
            pixleNum++;
        }

		BufferedImage image = ic.getImage();
		ImageIO.write(image, "png", outputFile);

		long endTime = System.nanoTime();

		double deltaTime = (double) (endTime - startTime) / 1000000000.0;

		System.out.println("Encoded in: " + deltaTime + " seconds");
	}

}