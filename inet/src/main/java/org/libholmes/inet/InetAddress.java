// This file is part of libholmes.
// Copyright 2018 Graham Shaw.
// Distribution and modification are permitted within the terms of the
// GNU General Public License (version 3 or any later version).

package org.libholmes.inet;

import org.libholmes.OctetString;
import org.libholmes.Address;
import org.libholmes.ParseException;

/** An abstract base class to represent an Internet Protocol address. */
public abstract class InetAddress extends Address {
    /** Copy-construct an InetAddress from another InetAddress.
     * @param that the InetAddress to be copied
     */
    protected InetAddress(InetAddress that) {
        super(that);
    }

    /** Construct InetAddress from an OctetString.
     * @param content the required content
     */
    protected InetAddress(OctetString content) {
        super(content);
    }
}