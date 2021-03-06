// This file is part of libholmes.
// Copyright 2018-2019 Graham Shaw.
// Distribution and modification are permitted within the terms of the
// GNU General Public License (version 3 or any later version).

package org.libholmes.icmp;

import java.net.InetAddress;
import javax.json.JsonNumber;
import javax.json.JsonObject;

import org.libholmes.OctetReader;
import org.libholmes.OctetPattern;
import org.libholmes.AnalysisContext;
import org.libholmes.Artefact;
import org.libholmes.Fingerprint;
import org.libholmes.Matcher;

public class Icmp4EchoFingerprint extends Fingerprint {
    /** A constant used to indicate little-endian byte order. */
    public static final int LITTLE_ENDIAN = 0;

    /** A constant used to indicate big-endian byte order. */
    public static final int BIG_ENDIAN = -1;

    /** A constant to indicate that the identifier may take any value,
     * with no constraint between related messages. */
    private final int IDENT_ANY = -1;

    /** A constant to indicate that the identifier may take any value,
     * but that related messages must have the same value. */
    private final int IDENT_FIXED = -2;

    /** A constant to indicate that the sequence number may take any value,
     * with no constraint between related messages. */
    private final int SN_ANY = -1;

    /** A constant to indicate that the sequence number should increment
     * for each message sent. */
    private final int SN_STEP = -2;

    /** True if length suffix appended to ID, otherwise false. */
    private final boolean lengthSuffix;

    /** The checksum which must be matched, or null for any value.
     * This is included for matching tools such as Paris Traceroute which
     * manipulate the payload in order to obtain a particular checksum.
     */
    private final Integer checksum;

    /** The identifier which must be matched, or an IDENT_ constant. */
    private final int identifier;

    /** The sequence number which must be matched, or null for any value. */
    private final int sequenceNumber;

    /** The byte order of the sequence number. */
    private final int sequenceByteOrder;

    /** The pattern which the data field must match in full. */
    private final OctetPattern dataPattern;

    /** Construct ICMPv4 echo fingerprint from JSON.
     * @param json the fingerprint, as JSON
     */
    public Icmp4EchoFingerprint(JsonObject json) {
        lengthSuffix = json.getBoolean("lengthSuffix", false);
        checksum = json.containsKey("checksum") ?
            new Integer(json.getInt("checksum")): null;

        if (json.containsKey("identifier")) {
            JsonObject identSpec = json.getJsonObject("identifier");
            String identType = identSpec.getString("type");
            if (identType.equals("fixed") || identType.equals("pid")) {
                identifier = identSpec.getInt("value", IDENT_FIXED);
            } else {
                identifier = IDENT_ANY;
            }
        } else {
            identifier = IDENT_ANY;
        }

        if (json.containsKey("sequenceNumber")) {
            JsonObject snSpec = json.getJsonObject("sequenceNumber");
            String snType = snSpec.getString("type");
            if (snType.equals("step")) {
                sequenceNumber = SN_STEP;
            } else {
                sequenceNumber = SN_ANY;
            }
            String byteOrderString = snSpec.getString("byteOrder", "");
            if (byteOrderString.equals("") || byteOrderString.equals("network")) {
                sequenceByteOrder = BIG_ENDIAN;
            } else if (byteOrderString.equals("host")) {
                sequenceByteOrder = LITTLE_ENDIAN;
            } else {
                throw new RuntimeException(
                    "invalid byteOrder for Icmp4EchoFingerprint");
            }
        } else {
            sequenceNumber = SN_ANY;
            sequenceByteOrder = BIG_ENDIAN;
        }

        dataPattern = OctetPattern.parse(json.get("data"));
    }

    /** Determine whether this fingerprint matches a given ICMPv4 message.
     * @param message the message against which to match
     * @param context the pattern matching context
     * @return true if fingerprint matches, otherwise false
     */
    public final boolean matches(Artefact artefact, AnalysisContext context) {
        Icmp4Message message = artefact.find(Icmp4Message.class);
        if (message == null) {
            return false;
        }

        if ((checksum != null) && (message.getRecordedChecksum() != checksum)) {
            return false;
        }
        if (message instanceof Icmp4EchoMessage) {
            Icmp4EchoMessage request = (Icmp4EchoMessage) message;
            if ((identifier >= 0) && (request.getIdentifier() != identifier)) {
                return false;
            }
            if ((sequenceNumber >= 0) && (request.getSequenceNumber() != sequenceNumber)) {
                return false;
            }
            OctetReader reader = request.getData().makeOctetReader();
            if (!dataPattern.matches(reader, context)) {
                return false;
            }
            if (reader.hasRemaining()) {
                return false;
            }
        } else if (message instanceof Icmp4EchoReplyMessage) {
            Icmp4EchoReplyMessage reply = (Icmp4EchoReplyMessage) message;
            if ((identifier >= 0) && (reply.getIdentifier() != identifier)) {
                return false;
            }
            if ((sequenceNumber >= 0) && (reply.getSequenceNumber() != sequenceNumber)) {
                return false;
            }
            OctetReader reader = reply.getData().makeOctetReader();
            if (!dataPattern.matches(reader, context)) {
                return false;
            }
            if (reader.hasRemaining()) {
                return false;
            }
        } else {
            return false;
        }
        return true;
    }

    @Override
    public final Matcher createMatcher() {
        Matcher identMatcher = null;
        if (identifier == IDENT_FIXED) {
            identMatcher = new Icmp4EchoFixedIdentifierMatcher();
        }

        Matcher snMatcher = null;
        if (sequenceNumber == SN_STEP) {
            snMatcher = new Icmp4EchoStepSequenceMatcher(sequenceByteOrder);
        }

        return new Icmp4EchoMatcher(identMatcher, snMatcher);
    }
}
