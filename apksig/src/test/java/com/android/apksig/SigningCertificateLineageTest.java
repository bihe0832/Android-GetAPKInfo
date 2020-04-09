/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.apksig;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.android.apksig.apk.ApkFormatException;
import com.android.apksig.internal.apk.ApkSigningBlockUtils;
import com.android.apksig.internal.apk.v3.V3SchemeSigner;
import com.android.apksig.internal.util.ByteBufferDataSource;
import com.android.apksig.internal.util.ByteBufferUtils;
import com.android.apksig.internal.util.Resources;

import com.android.apksig.SigningCertificateLineage.SignerConfig;
import com.android.apksig.SigningCertificateLineage.SignerCapabilities;

import com.android.apksig.util.DataSource;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.nio.ByteOrder;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RunWith(JUnit4.class)
public class SigningCertificateLineageTest {

    // createLineageWithSignersFromResources and updateLineageWithSignerFromResources will add the
    // SignerConfig for the signers added to the Lineage to this list.
    private List<SignerConfig> mSigners;

    // All signers with the same prefix and an _X suffix were signed with the private key of the
    // (X-1) signer.
    private static final String FIRST_RSA_1024_SIGNER_RESOURCE_NAME = "rsa-1024";
    private static final String SECOND_RSA_1024_SIGNER_RESOURCE_NAME = "rsa-1024_2";

    private static final String FIRST_RSA_2048_SIGNER_RESOURCE_NAME = "rsa-2048";
    private static final String SECOND_RSA_2048_SIGNER_RESOURCE_NAME = "rsa-2048_2";
    private static final String THIRD_RSA_2048_SIGNER_RESOURCE_NAME = "rsa-2048_3";

    @Before
    public void setUp() {
        mSigners = new ArrayList<>();
    }

    @Test
    public void testFirstRotationContainsExpectedSigners() throws Exception {
        SigningCertificateLineage lineage = createLineageWithSignersFromResources(
                FIRST_RSA_2048_SIGNER_RESOURCE_NAME, SECOND_RSA_2048_SIGNER_RESOURCE_NAME);
        assertLineageContainsExpectedSigners(lineage, mSigners);
        SignerConfig unknownSigner = Resources.toLineageSignerConfig(getClass(),
                THIRD_RSA_2048_SIGNER_RESOURCE_NAME);
        assertFalse("The signer " + unknownSigner.getCertificate().getSubjectDN()
                + " should not be in the lineage", lineage.isSignerInLineage(unknownSigner));
    }

    @Test
    public void testRotationWithExistingLineageContainsExpectedSigners() throws Exception {
        SigningCertificateLineage lineage = createLineageWithSignersFromResources(
                FIRST_RSA_2048_SIGNER_RESOURCE_NAME, SECOND_RSA_2048_SIGNER_RESOURCE_NAME);
        lineage = updateLineageWithSignerFromResources(lineage,
                THIRD_RSA_2048_SIGNER_RESOURCE_NAME);
        assertLineageContainsExpectedSigners(lineage, mSigners);
    }

    @Test
    public void testLineageFromFileContainsExpectedSigners() throws Exception {
        // This file contains the lineage with the three rsa-2048 signers
        DataSource lineageDataSource = Resources.toDataSource(getClass(),
                "rsa-2048-lineage-3-signers");
        SigningCertificateLineage lineage = SigningCertificateLineage.readFromDataSource(
                lineageDataSource);
        List<SignerConfig> signers = new ArrayList<>(3);
        signers.add(
                Resources.toLineageSignerConfig(getClass(), FIRST_RSA_2048_SIGNER_RESOURCE_NAME));
        signers.add(
                Resources.toLineageSignerConfig(getClass(), SECOND_RSA_2048_SIGNER_RESOURCE_NAME));
        signers.add(
                Resources.toLineageSignerConfig(getClass(), THIRD_RSA_2048_SIGNER_RESOURCE_NAME));
        assertLineageContainsExpectedSigners(lineage, signers);
    }

    @Test
    public void testLineageFromFileDoesNotContainUnknownSigner() throws Exception {
        // This file contains the lineage with the first two rsa-2048 signers
        SigningCertificateLineage lineage = Resources.toSigningCertificateLineage(getClass(),
                "rsa-2048-lineage-2-signers");
        SignerConfig unknownSigner = Resources.toLineageSignerConfig(getClass(),
                THIRD_RSA_2048_SIGNER_RESOURCE_NAME);
        assertFalse("The signer " + unknownSigner.getCertificate().getSubjectDN()
                + " should not be in the lineage", lineage.isSignerInLineage(unknownSigner));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLineageFromFileWithInvalidMagicFails() throws Exception {
        // This file contains the lineage with two rsa-2048 signers and a modified MAGIC value
        Resources.toSigningCertificateLineage(getClass(), "rsa-2048-lineage-invalid-magic");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLineageFromFileWithInvalidVersionFails() throws Exception {
        // This file contains the lineage with two rsa-2048 signers and an invalid value of FF for
        // the version
        Resources.toSigningCertificateLineage(getClass(), "rsa-2048-lineage-invalid-version");
    }

    @Test
    public void testLineageWrittenToFileContainsExpectedSigners() throws Exception {
        SigningCertificateLineage lineage = createLineageWithSignersFromResources(
                FIRST_RSA_2048_SIGNER_RESOURCE_NAME, SECOND_RSA_2048_SIGNER_RESOURCE_NAME);
        lineage = updateLineageWithSignerFromResources(lineage,
                THIRD_RSA_2048_SIGNER_RESOURCE_NAME);
        File lineageFile = File.createTempFile(getClass().getSimpleName(), ".bin");
        lineageFile.deleteOnExit();
        lineage.writeToFile(lineageFile);
        lineage = SigningCertificateLineage.readFromFile(lineageFile);
        assertLineageContainsExpectedSigners(lineage, mSigners);
    }

    @Test
    public void testUpdatedCapabilitiesInLineage() throws Exception {
        SigningCertificateLineage lineage = createLineageWithSignersFromResources(
                FIRST_RSA_2048_SIGNER_RESOURCE_NAME, SECOND_RSA_2048_SIGNER_RESOURCE_NAME);
        SignerConfig oldSignerConfig = mSigners.get(0);
        List<Boolean> expectedCapabilityValues = Arrays.asList(false, false, false, false, false);
        SignerCapabilities newCapabilities = buildSignerCapabilities(expectedCapabilityValues);
        lineage.updateSignerCapabilities(oldSignerConfig, newCapabilities);
        SignerCapabilities updatedCapabilities = lineage.getSignerCapabilities(oldSignerConfig);
        assertExpectedCapabilityValues(updatedCapabilities, expectedCapabilityValues);
    }

    @Test
    public void testUpdatedCapabilitiesInLineageWrittenToFile() throws Exception {
        SigningCertificateLineage lineage = createLineageWithSignersFromResources(
                FIRST_RSA_2048_SIGNER_RESOURCE_NAME, SECOND_RSA_2048_SIGNER_RESOURCE_NAME);
        SignerConfig oldSignerConfig = mSigners.get(0);
        List<Boolean> expectedCapabilityValues = Arrays.asList(false, false, false, false, false);
        SignerCapabilities newCapabilities = buildSignerCapabilities(expectedCapabilityValues);
        lineage.updateSignerCapabilities(oldSignerConfig, newCapabilities);
        File lineageFile = File.createTempFile(getClass().getSimpleName(), ".bin");
        lineageFile.deleteOnExit();
        lineage.writeToFile(lineageFile);
        lineage = SigningCertificateLineage.readFromFile(lineageFile);
        SignerCapabilities updatedCapabilities = lineage.getSignerCapabilities(oldSignerConfig);
        assertExpectedCapabilityValues(updatedCapabilities, expectedCapabilityValues);
    }

    @Test
    public void testCapabilitiesAreNotUpdatedWithDefaultValues() throws Exception {
        // This file contains the lineage with the first two rsa-2048 signers with the first signer
        // having all of the capabilities set to false.
        SigningCertificateLineage lineage = Resources.toSigningCertificateLineage(getClass(),
                "rsa-2048-lineage-no-capabilities-first-signer");
        List<Boolean> expectedCapabilityValues = Arrays.asList(false, false, false, false, false);
        SignerConfig oldSignerConfig = Resources.toLineageSignerConfig(getClass(),
                FIRST_RSA_2048_SIGNER_RESOURCE_NAME);
        SignerCapabilities oldSignerCapabilities = lineage.getSignerCapabilities(oldSignerConfig);
        assertExpectedCapabilityValues(oldSignerCapabilities, expectedCapabilityValues);
        // The builder is called directly to ensure all of the capabilities are set to the default
        // values and the caller configured flags are not modified in this SignerCapabilities.
        SignerCapabilities newCapabilities = new SignerCapabilities.Builder().build();
        lineage.updateSignerCapabilities(oldSignerConfig, newCapabilities);
        SignerCapabilities updatedCapabilities = lineage.getSignerCapabilities(oldSignerConfig);
        assertExpectedCapabilityValues(updatedCapabilities, expectedCapabilityValues);
    }

    @Test
    public void testFirstRotationWitNonDefaultCapabilitiesForSigners() throws Exception {
        SignerConfig oldSigner = Resources.toLineageSignerConfig(getClass(),
                FIRST_RSA_2048_SIGNER_RESOURCE_NAME);
        SignerConfig newSigner = Resources.toLineageSignerConfig(getClass(),
                SECOND_RSA_2048_SIGNER_RESOURCE_NAME);
        List<Boolean> oldSignerCapabilityValues = Arrays.asList(false, false, false, false, false);
        List<Boolean> newSignerCapabilityValues = Arrays.asList(false, true, false, false, false);
        SigningCertificateLineage lineage = new SigningCertificateLineage.Builder(oldSigner,
                newSigner)
                .setOriginalCapabilities(buildSignerCapabilities(oldSignerCapabilityValues))
                .setNewCapabilities(buildSignerCapabilities(newSignerCapabilityValues))
                .build();
        SignerCapabilities oldSignerCapabilities = lineage.getSignerCapabilities(oldSigner);
        assertExpectedCapabilityValues(oldSignerCapabilities, oldSignerCapabilityValues);
        SignerCapabilities newSignerCapabilities = lineage.getSignerCapabilities(newSigner);
        assertExpectedCapabilityValues(newSignerCapabilities, newSignerCapabilityValues);
    }

    @Test
    public void testRotationWithExitingLineageAndNonDefaultCapabilitiesForNewSigner()
            throws Exception {
        SigningCertificateLineage lineage = createLineageWithSignersFromResources(
                FIRST_RSA_2048_SIGNER_RESOURCE_NAME, SECOND_RSA_2048_SIGNER_RESOURCE_NAME);
        SignerConfig oldSigner = mSigners.get(mSigners.size() - 1);
        SignerConfig newSigner = Resources.toLineageSignerConfig(getClass(),
                THIRD_RSA_2048_SIGNER_RESOURCE_NAME);
        List<Boolean> newSignerCapabilityValues = Arrays.asList(false, false, false, false, false);
        lineage = lineage.spawnDescendant(oldSigner, newSigner,
                buildSignerCapabilities(newSignerCapabilityValues));
        SignerCapabilities newSignerCapabilities = lineage.getSignerCapabilities(newSigner);
        assertExpectedCapabilityValues(newSignerCapabilities, newSignerCapabilityValues);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRotationWithExistingLineageUsingNonParentSignerFails() throws Exception {
        // When rotating the signing certificate the most recent signer must be provided to the
        // spawnDescendant method. This test ensures that using an ancestor of the most recent
        // signer will fail as expected.
        SigningCertificateLineage lineage = createLineageWithSignersFromResources(
                FIRST_RSA_2048_SIGNER_RESOURCE_NAME, SECOND_RSA_2048_SIGNER_RESOURCE_NAME);
        SignerConfig oldestSigner = mSigners.get(0);
        SignerConfig newSigner = Resources.toLineageSignerConfig(getClass(),
                THIRD_RSA_2048_SIGNER_RESOURCE_NAME);
        lineage.spawnDescendant(oldestSigner, newSigner);
    }

    @Test
    public void testLineageFromV3SignerAttribute() throws Exception {
        SigningCertificateLineage lineage = createLineageWithSignersFromResources(
                FIRST_RSA_2048_SIGNER_RESOURCE_NAME, SECOND_RSA_2048_SIGNER_RESOURCE_NAME);
        // The format of the V3 Signer Attribute is as follows (little endian):
        // * length-prefixed bytes: attribute pair
        //   * uint32: ID
        //   * bytes: value - encoded V3 SigningCertificateLineage
        ByteBuffer v3SignerAttribute = ByteBuffer.wrap(lineage.generateV3SignerAttribute());
        v3SignerAttribute.order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer attribute = ApkSigningBlockUtils.getLengthPrefixedSlice(v3SignerAttribute);
        // The generateV3SignerAttribute method should only use the PROOF_OF_ROTATION_ATTR_ID
        // value for the ID.
        int id = attribute.getInt();
        assertEquals(
                "The ID of the v3SignerAttribute ByteBuffer is not the expected "
                        + "PROOF_OF_ROTATION_ATTR_ID",
                V3SchemeSigner.PROOF_OF_ROTATION_ATTR_ID, id);
        lineage = SigningCertificateLineage.readFromV3AttributeValue(
                ByteBufferUtils.toByteArray(attribute));
        assertLineageContainsExpectedSigners(lineage, mSigners);
    }

    @Test
    public void testSortedSignerConfigsAreInSortedOrder() throws Exception {
        SigningCertificateLineage lineage = createLineageWithSignersFromResources(
                FIRST_RSA_2048_SIGNER_RESOURCE_NAME, SECOND_RSA_2048_SIGNER_RESOURCE_NAME);
        DefaultApkSignerEngine.SignerConfig oldSigner = getApkSignerEngineSignerConfigFromResources(
                FIRST_RSA_2048_SIGNER_RESOURCE_NAME);
        DefaultApkSignerEngine.SignerConfig newSigner = getApkSignerEngineSignerConfigFromResources(
                SECOND_RSA_2048_SIGNER_RESOURCE_NAME);
        List<DefaultApkSignerEngine.SignerConfig> signers = Arrays.asList(newSigner, oldSigner);
        List<DefaultApkSignerEngine.SignerConfig> sortedSigners = lineage.sortSignerConfigs(
                signers);
        assertEquals("The sorted signer list does not contain the expected number of elements",
                signers.size(), sortedSigners.size());
        assertEquals("The first element in the sorted list should be the first signer", oldSigner,
                sortedSigners.get(0));
        assertEquals("The second element in the sorted list should be the second signer", newSigner,
                sortedSigners.get(1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSortedSignerConfigsWithUnknownSignerFails() throws Exception {
        // Since this test includes a signer that is not in the lineage the sort should fail with
        // an IllegalArgumentException.
        SigningCertificateLineage lineage = createLineageWithSignersFromResources(
                FIRST_RSA_2048_SIGNER_RESOURCE_NAME, SECOND_RSA_2048_SIGNER_RESOURCE_NAME);
        DefaultApkSignerEngine.SignerConfig oldSigner = getApkSignerEngineSignerConfigFromResources(
                FIRST_RSA_2048_SIGNER_RESOURCE_NAME);
        DefaultApkSignerEngine.SignerConfig newSigner = getApkSignerEngineSignerConfigFromResources(
                SECOND_RSA_2048_SIGNER_RESOURCE_NAME);
        DefaultApkSignerEngine.SignerConfig unknownSigner =
                getApkSignerEngineSignerConfigFromResources(THIRD_RSA_2048_SIGNER_RESOURCE_NAME);
        List<DefaultApkSignerEngine.SignerConfig> signers = Arrays.asList(newSigner, oldSigner,
                unknownSigner);
        lineage.sortSignerConfigs(signers);
    }

    @Test
    public void testAllExpectedCertificatesAreInLineage() throws Exception {
        SigningCertificateLineage lineage = createLineageWithSignersFromResources(
                FIRST_RSA_2048_SIGNER_RESOURCE_NAME, SECOND_RSA_2048_SIGNER_RESOURCE_NAME);
        lineage = updateLineageWithSignerFromResources(lineage,
                THIRD_RSA_2048_SIGNER_RESOURCE_NAME);
        Set<X509Certificate> expectedCertSet = new HashSet<>();
        for (int i = 0; i < mSigners.size(); i++) {
            expectedCertSet.add(mSigners.get(i).getCertificate());
        }
        List<X509Certificate> certs = lineage.getCertificatesInLineage();
        assertEquals(
                "The number of elements in the certificate list from the lineage does not equal "
                        + "the expected number",
                expectedCertSet.size(), certs.size());
        for (X509Certificate cert : certs) {
            // remove the certificate from the Set to ensure duplicate certs were not returned.
            assertTrue("An unexpected certificate, " + cert.getSubjectDN() + ", is in the lineage",
                    expectedCertSet.remove(cert));
        }
    }

    @Test
    public void testSublineageContainsExpectedSigners() throws Exception {
        SigningCertificateLineage lineage = createLineageWithSignersFromResources(
                FIRST_RSA_2048_SIGNER_RESOURCE_NAME, SECOND_RSA_2048_SIGNER_RESOURCE_NAME);
        lineage = updateLineageWithSignerFromResources(lineage,
                THIRD_RSA_2048_SIGNER_RESOURCE_NAME);
        List<SignerConfig> subList = mSigners.subList(0, 2);
        X509Certificate cert = subList.get(1).getCertificate();
        SigningCertificateLineage subLineage = lineage.getSubLineage(cert);
        assertLineageContainsExpectedSigners(subLineage, subList);
    }

    @Test
    public void testConsolidatedLineageContainsExpectedSigners() throws Exception {
        SigningCertificateLineage lineage = createLineageWithSignersFromResources(
                FIRST_RSA_2048_SIGNER_RESOURCE_NAME, SECOND_RSA_2048_SIGNER_RESOURCE_NAME);
        SigningCertificateLineage updatedLineage = updateLineageWithSignerFromResources(lineage,
                THIRD_RSA_2048_SIGNER_RESOURCE_NAME);
        List<SigningCertificateLineage> lineages = Arrays.asList(lineage, updatedLineage);
        SigningCertificateLineage consolidatedLineage =
                SigningCertificateLineage.consolidateLineages(lineages);
        assertLineageContainsExpectedSigners(consolidatedLineage, mSigners);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConsolidatedLineageWithDisjointLineagesFail() throws Exception {
        List<SigningCertificateLineage> lineages = new ArrayList<>();
        lineages.add(createLineageWithSignersFromResources(FIRST_RSA_1024_SIGNER_RESOURCE_NAME,
                SECOND_RSA_1024_SIGNER_RESOURCE_NAME));
        lineages.add(createLineageWithSignersFromResources(FIRST_RSA_2048_SIGNER_RESOURCE_NAME,
                SECOND_RSA_2048_SIGNER_RESOURCE_NAME));
        SigningCertificateLineage.consolidateLineages(lineages);
    }

    @Test
    public void testLineageFromAPKContainsExpectedSigners() throws Exception {
        SignerConfig firstSigner = getSignerConfigFromResources(
                FIRST_RSA_2048_SIGNER_RESOURCE_NAME);
        SignerConfig secondSigner = getSignerConfigFromResources(
                SECOND_RSA_2048_SIGNER_RESOURCE_NAME);
        SignerConfig thirdSigner = getSignerConfigFromResources(
                THIRD_RSA_2048_SIGNER_RESOURCE_NAME);
        List<SignerConfig> expectedSigners = Arrays.asList(firstSigner, secondSigner, thirdSigner);
        DataSource apkDataSource = Resources.toDataSource(getClass(),
                "v1v2v3-with-rsa-2048-lineage-3-signers.apk");
        SigningCertificateLineage lineageFromApk = SigningCertificateLineage.readFromApkDataSource(
                apkDataSource);
        assertLineageContainsExpectedSigners(lineageFromApk, expectedSigners);
    }

    @Test(expected = ApkFormatException.class)
    public void testLineageFromAPKWithInvalidZipCDSizeFails() throws Exception {
        // This test verifies that attempting to read the lineage from an APK where the zip
        // sections cannot be parsed fails. This APK is based off the
        // v1v2v3-with-rsa-2048-lineage-3-signers.apk with a modified CD size in the EoCD.
        DataSource apkDataSource = Resources.toDataSource(getClass(),
                "v1v2v3-with-rsa-2048-lineage-3-signers-invalid-zip.apk");
        SigningCertificateLineage.readFromApkDataSource(apkDataSource);
    }

    @Test
    public void testLineageFromAPKWithNoLineageFails() throws Exception {
        // This test verifies that attempting to read the lineage from an APK without a lineage
        // fails.
        // This is a valid APK that has only been signed with the V1 and V2 signature schemes;
        // since the lineage is an attribute in the V3 signature block this test should fail.
        DataSource apkDataSource = Resources.toDataSource(getClass(),
                "golden-aligned-v1v2-out.apk");
        try {
            SigningCertificateLineage.readFromApkDataSource(apkDataSource);
            fail("A failure should have been reported due to the APK not containing a V3 signing "
                    + "block");
        } catch (IllegalArgumentException expected) {}

        // This is a valid APK signed with the V1, V2, and V3 signature schemes, but there is no
        // lineage in the V3 signature block.
        apkDataSource = Resources.toDataSource(getClass(), "golden-aligned-v1v2v3-out.apk");
        try {
            SigningCertificateLineage.readFromApkDataSource(apkDataSource);
            fail("A failure should have been reported due to the APK containing a V3 signing "
                    + "block without the lineage attribute");
        } catch (IllegalArgumentException expected) {}

        // This APK is based off the v1v2v3-with-rsa-2048-lineage-3-signers.apk with a bit flip
        // in the lineage attribute ID in the V3 signature block.
        apkDataSource = Resources.toDataSource(getClass(),
                "v1v2v3-with-rsa-2048-lineage-3-signers-invalid-lineage-attr.apk");
        try {
            SigningCertificateLineage.readFromApkDataSource(apkDataSource);
            fail("A failure should have been reported due to the APK containing a V3 signing "
                    + "block with a modified lineage attribute ID");
        } catch (IllegalArgumentException expected) {}
    }

    /**
     * Builds a new {@code SigningCertificateLinage.SignerCapabilities} object using the values in
     * the provided {@code List}. The {@code List} should contain {@code boolean} values to be
     * passed to the following methods in the
     * {@code SigningCertificateLineage.SignerCapabilities.Builder} (if a value is not provided the
     * noted default is used):
     *
     *  {@code SigningCertificateLineage.SignerCapabilities.Builder.setInstalledData} [{@code true}]
     *  {@code SigningCertificateLineage.SignerCapabilities.Builder.setSharedUid} [{@code true}]
     *  {@code SigningCertificateLineage.SignerCapabilities.Builder.setPermission} [{@code true}]
     *  {@code SigningCertificateLineage.SignerCapabilities.Builder.setRollback} [{@code false}]
     *  {@code SigningCertificateLineage.SignerCapabilities.Builder.setAuth} [{@code true}]
     *
     * This method should not be used when testing caller configured capabilities since the setXX
     * method for each capability is called.
     */
    private SignerCapabilities buildSignerCapabilities(List<Boolean> capabilityValues) {
        return new SignerCapabilities.Builder()
                .setInstalledData(capabilityValues.size() > 0 ? capabilityValues.get(0) : true)
                .setSharedUid(capabilityValues.size() > 1 ? capabilityValues.get(1) : true)
                .setPermission(capabilityValues.size() > 2 ? capabilityValues.get(2) : true)
                .setRollback(capabilityValues.size() > 3 ? capabilityValues.get(3) : false)
                .setAuth(capabilityValues.size() > 4 ? capabilityValues.get(4) : true)
                .build();
    }

    /**
     * Verifies the specified {@code SigningCertificateLinage.SignerCapabilities} contains the
     * expected values from the provided {@code List}. The {@code List} should contain
     * {@code boolean} values to be verified against the
     * {@code SigningCertificateLinage.SignerCapabilities} methods in the following order:
     *
     *  {@mcode SigningCertificateLineage.SignerCapabilities.hasInstalledData}
     *  {@mcode SigningCertificateLineage.SignerCapabilities.hasSharedUid}
     *  {@mcode SigningCertificateLineage.SignerCapabilities.hasPermission}
     *  {@mcode SigningCertificateLineage.SignerCapabilities.hasRollback}
     *  {@mcode SigningCertificateLineage.SignerCapabilities.hasAuth}
     */
    private void assertExpectedCapabilityValues(SignerCapabilities capabilities,
            List<Boolean> expectedCapabilityValues) {
        assertTrue("The expectedCapabilityValues do not contain the expected number of elements",
                expectedCapabilityValues.size() >= 5);
        assertEquals(
                "The installed data capability is not set to the expected value",
                expectedCapabilityValues.get(0), capabilities.hasInstalledData());
        assertEquals(
                "The shared UID capability is not set to the expected value",
                expectedCapabilityValues.get(1), capabilities.hasSharedUid());
        assertEquals(
                "The permission capability is not set to the expected value",
                expectedCapabilityValues.get(2), capabilities.hasPermission());
        assertEquals(
                "The rollback capability is not set to the expected value",
                expectedCapabilityValues.get(3), capabilities.hasRollback());
        assertEquals(
                "The auth capability is not set to the expected value",
                expectedCapabilityValues.get(4), capabilities.hasAuth());
    }

    /**
     * Creates a new {@code SigningCertificateLineage} with the specified signers from the
     * resources. {@code mSigners} will be updated with the
     * {@code SigningCertificateLineage.SignerConfig} for each signer added to the lineage.
     */
    private SigningCertificateLineage createLineageWithSignersFromResources(
            String oldSignerResourceName, String newSignerResourceName) throws Exception {
        SignerConfig oldSignerConfig = Resources.toLineageSignerConfig(getClass(),
                oldSignerResourceName);
        mSigners.add(oldSignerConfig);
        SignerConfig newSignerConfig = Resources.toLineageSignerConfig(getClass(),
                newSignerResourceName);
        mSigners.add(newSignerConfig);
        return new SigningCertificateLineage.Builder(oldSignerConfig, newSignerConfig).build();
    }

    /**
     * Updates the specified {@code SigningCertificateLineage} with the signer from the resources.
     * Requires that the {@code mSigners} list contains the previous signers in the lineage since
     * the most recent signer must be specified when adding a new signer to the lineage.
     */
    private SigningCertificateLineage updateLineageWithSignerFromResources(
            SigningCertificateLineage lineage, String newSignerResourceName) throws Exception {
        // To add a new Signer to an existing lineage the config of the last signer must be
        // specified. If this class was used to create the lineage then the last signer should
        // be in the mSigners list.
        assertTrue("The mSigners list did not contain the expected signers to update the lineage",
                mSigners.size() >= 2);
        SignerConfig oldSignerConfig = mSigners.get(mSigners.size() - 1);
        SignerConfig newSignerConfig = Resources.toLineageSignerConfig(getClass(),
                newSignerResourceName);
        mSigners.add(newSignerConfig);
        return lineage.spawnDescendant(oldSignerConfig, newSignerConfig);
    }

    private void assertLineageContainsExpectedSigners(SigningCertificateLineage lineage,
            List<SignerConfig> signers) {
        assertEquals("The lineage does not contain the expected number of signers",
                signers.size(), lineage.size());
        for (SignerConfig signer : signers) {
            assertTrue("The signer " + signer.getCertificate().getSubjectDN()
                    + " is expected to be in the lineage", lineage.isSignerInLineage(signer));
        }
    }

    private static SignerConfig getSignerConfigFromResources(
            String resourcePrefix) throws Exception {
        PrivateKey privateKey =
                Resources.toPrivateKey(SigningCertificateLineageTest.class,
                        resourcePrefix + ".pk8");
        X509Certificate cert = Resources.toCertificate(SigningCertificateLineageTest.class,
                resourcePrefix + ".x509.pem");
        return new SignerConfig.Builder(privateKey, cert).build();
    }

    private static DefaultApkSignerEngine.SignerConfig getApkSignerEngineSignerConfigFromResources(
            String resourcePrefix) throws Exception {
        PrivateKey privateKey =
                Resources.toPrivateKey(SigningCertificateLineageTest.class,
                        resourcePrefix + ".pk8");
        X509Certificate cert = Resources.toCertificate(SigningCertificateLineageTest.class,
                resourcePrefix + ".x509.pem");
        return new DefaultApkSignerEngine.SignerConfig.Builder(resourcePrefix, privateKey,
                Collections.singletonList(cert)).build();
    }
}
