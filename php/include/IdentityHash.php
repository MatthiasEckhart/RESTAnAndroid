<?php

class IdentityHash {

    // private key
	private static $privateKEY = "secret-of-mike";

    // this will be used to generate a hash
    public static function hash($values) {
	
		$secret = base64_decode(self::$privateKEY);
		$hmac  = hash_hmac('sha1', $values, $secret, true);
		$hmac64 = base64_encode($hmac);
		
		return $hmac64;
    }

    // this will be used to compare a the generated hash against an incoming hash
    public static function check_hash($hash, $incoming_hash) {
        return ($hash == $incoming_hash);
    }

}
