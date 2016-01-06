package util;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.SecureRandom;
import java.security.Security;

/**
 * Please note that this class is not needed for Lab 1, but can later be
 * used in Lab 2.
 * 
 * Provides security provider related utility methods.
 */
public final class SecurityUtils {
	private static SecureRandom secureRandom = new SecureRandom();

	private SecurityUtils() {
	}

	/**
	 * Registers the {@link BouncyCastleProvider} as the primary security
	 * provider if necessary.
	 */
	public static synchronized void registerBouncyCastle() {
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
			Security.insertProviderAt(new BouncyCastleProvider(), 0);
		}
	}

	public static synchronized byte[] getSecureRandom(){
		// generates a 32 byte secure random number
		final byte[] number = new byte[32];
		secureRandom.nextBytes(number);
		return number;
	}
	public static synchronized byte[] getSecureRandomSmall(){
		// generates a 32 byte secure random number
		final byte[] number = new byte[16];
		secureRandom.nextBytes(number);
		return number;
	}
}
