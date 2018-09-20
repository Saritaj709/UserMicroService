package com.bridgelabz.microservices.user.services;

import java.util.List;

import com.amazonaws.services.s3.model.Bucket;

public interface AwsS3Service {
String createBucket(String bucketName);
List<Bucket> showBucket();
String createFolderInBucket(String bucketName,String folderName);
String uploadFile(String bucketName,String folderName,String file);
String deleteBucket(String bucketName);
String deleteFolderAndFile(String bucketName, String folderName);
String deleteImagesFromNote(String image);
String deleteImagesFromUser(String image);

}
