package org.wildfly.extras.sunstone.api.impl;

import com.google.common.base.Strings;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.domain.Image;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Resolves image ID for cloud providers that offer both a unique image ID and potentially ambiguous but human-readable
 * image name. Known to work for the OpenStack provider. Might work for the EC2 provider. Useless for providers
 * that don't have a distinct ID and name, though it could be made useful for them too.
 */
public final class ResolvedImage {
    /** {@code region/imageId}, can be directly used with the cloud provider */
    public final String fullId;
    /** human-readable name of the image in an arbitrary, unspecified form */
    public final String humanReadableName;

    private ResolvedImage(String fullId, String humanReadableName) {
        this.fullId = fullId;
        this.humanReadableName = humanReadableName;
    }

    /**
     * @param imageName         human-readable image name; if {@code null} or empty, the entire name resolution logic
     *                          is ignored and a combination of {@code region/imageId} is used
     * @param imageId           unique image ID; used when {@code imageName} is missing or ambiguous
     * @param region            already resolved region/location ID; expected to be correct, no checks are made
     * @param computeService    the JClouds {@link ComputeService}
     */
    public static ResolvedImage fromNameAndId(String imageName, String imageId, String region,
                                              ComputeService computeService) {
        if (Strings.isNullOrEmpty(imageName)) {
            String fullId = region + "/" + imageId;
            Image image = computeService.getImage(fullId);
            if (image == null) {
                throw new IllegalArgumentException("No image exists: " + fullId);
            }
            return new ResolvedImage(fullId, fullId);
        }

        List<? extends Image> matchingImages = computeService.listImages()
                .stream()
                .filter(image -> imageName.equals(image.getName()))
                .collect(Collectors.toList());
        if (matchingImages.isEmpty()) {
            throw new IllegalArgumentException("No image exists: " + imageName);
        } else if (matchingImages.size() == 1) {
            Image image = matchingImages.get(0);

            if (!Strings.isNullOrEmpty(imageId) && !image.getProviderId().equals(imageId)) {
                throw new IllegalArgumentException("Image name '" + imageName + "' uniquely identifies an image (ID '"
                        + image.getProviderId() +"'), but the specified image ID is different: " + imageId);
            }

            String humanReadableName = imageName;
            if (!Strings.isNullOrEmpty(imageId)) {
                humanReadableName += " (" + image.getId() + ")";
            }
            return new ResolvedImage(image.getId(), humanReadableName);
        } else {
            if (Strings.isNullOrEmpty(imageId)) {
                throw new IllegalArgumentException("Ambiguous image name: " + imageName + ", use image.id");
            }

            List<? extends Image> nextMatchingImages = matchingImages.stream()
                    .filter(image -> imageId.equals(image.getProviderId()))
                    .collect(Collectors.toList());

            if (nextMatchingImages.isEmpty()) {
                throw new IllegalArgumentException("No image exists: " + imageName + " (" + imageId + ")");
            } else if (nextMatchingImages.size() == 1) {
                Image image = nextMatchingImages.get(0);
                return new ResolvedImage(image.getId(), imageName + " (" + image.getId() + ")");
            } else {
                // should never happen
                throw new IllegalArgumentException("Ambiguous image name: " + imageName + " and image ID: " + imageId);
            }
        }
    }
}
