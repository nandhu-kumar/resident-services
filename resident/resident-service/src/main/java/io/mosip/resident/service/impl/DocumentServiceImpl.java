package io.mosip.resident.service.impl;

import io.mosip.commons.khazana.dto.ObjectDto;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.UUIDUtils;
import io.mosip.resident.config.LoggerConfiguration;
import io.mosip.resident.constant.LoggerFileConstant;
import io.mosip.resident.constant.ResidentErrorCode;
import io.mosip.resident.dto.DocumentDTO;
import io.mosip.resident.dto.DocumentRequestDTO;
import io.mosip.resident.dto.DocumentResponseDTO;
import io.mosip.resident.dto.ResponseDTO;
import io.mosip.resident.exception.ResidentServiceCheckedException;
import io.mosip.resident.helper.ObjectStoreHelper;
import io.mosip.resident.service.DocumentService;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.velocity.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * It's a service class that uploads a document to the object store and returns
 * the document metadata.
 * 
 * @author Manoj SP
 */
@Service
public class DocumentServiceImpl implements DocumentService {

	private static final Logger logger = LoggerConfiguration.logConfig(DocumentServiceImpl.class);

	private static final String SUCCESS = "SUCCESS";
	private static final String DOCUMENT_DELETION_SUCCESS_MESSAGE = "Document deleted successfully";
	private static final String FAILURE = "FAILURE";
	private static final String DOCUMENT_DELETION_FAILURE_MESSAGE = "Document deletion failed";

	@Autowired
	private ObjectStoreHelper objectStoreHelper;

	/**
	 * It uploads a file to the object store
	 * 
	 * @param transactionId This is the transaction id of the resident.
	 * @param file          The file to be uploaded
	 * @param request       DocumentRequestDTO
	 * @return The response is a DocumentResponseDTO object.
	 */
	@Override
	public DocumentResponseDTO uploadDocument(String transactionId, MultipartFile file, DocumentRequestDTO request)
			throws ResidentServiceCheckedException {
		try {
			String docId = UUIDUtils.getUUID(UUIDUtils.NAMESPACE_OID, transactionId + request.getDocCatCode()).toString();
			String objectNameWithPath = transactionId + "/" + docId;
			Map<String, Object> metadata = Map.of("doccatcode", request.getDocCatCode(), "doctypcode",
					request.getDocTypCode(), "langcode", request.getLangCode(), "docname", file.getOriginalFilename(),
					"docid", docId);
			objectStoreHelper.putObject(objectNameWithPath, file.getInputStream(), metadata);
			DocumentResponseDTO response = new DocumentResponseDTO();
			response.setTransactionId(transactionId);
			response.setDocId(docId);
			response.setDocName(file.getOriginalFilename());
			response.setDocCatCode(request.getDocCatCode());
			response.setDocTypCode(request.getDocTypCode());
			response.setDocFileFormat(StringUtils.split(file.getOriginalFilename(), "\\.")[1]);
			return response;
		} catch (IOException e) {
			logger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.APPLICATIONID.toString(),
					LoggerFileConstant.APPLICATIONID.toString(), ExceptionUtils.getStackTrace(e));
			throw new ResidentServiceCheckedException(ResidentErrorCode.FAILED_TO_UPLOAD_DOC.getErrorCode(),
					ResidentErrorCode.FAILED_TO_UPLOAD_DOC.getErrorMessage(), e);
		}
	}

	/**
	 * It fetches all the documents metadata from the object store
	 * 
	 * @param transactionId This is the transaction ID that is generated by the
	 *                      resident service.
	 * @return A list of DocumentResponseDTO objects.
	 */
	@Override
	public List<DocumentResponseDTO> fetchAllDocumentsMetadata(String transactionId)
			throws ResidentServiceCheckedException {
		List<ObjectDto> allObjects = objectStoreHelper.getAllObjects(transactionId);
		return allObjects.stream().map(object -> this.fetchDocumentMetadata(transactionId, object.getObjectName()))
				.collect(Collectors.toList());
	}

	/**
	 * It fetches the document  from the object store
	 * @param transactionId This is the transaction ID
	 * @param documentId This is the document id
	 */

	@Override
	public DocumentDTO fetchDocumentByDocId(String transactionId, String documentId) throws ResidentServiceCheckedException {
		DocumentDTO document = new DocumentDTO();
		String objectNameWithPath = transactionId + "/" + documentId;
		String sourceFile = objectStoreHelper.getObject(objectNameWithPath);
		document.setDocument(sourceFile.getBytes(StandardCharsets.UTF_8));
		return document;
	}

	/**
	 * It fetches all the documents from the object store and returns a map of
	 * document metadata and document content
	 * 
	 * @param transactionId The transaction ID of the transaction for which the
	 *                      documents are to be fetched.
	 * @return A map of DocumentResponseDTO and String.
	 */
	@Override
	public Map<DocumentResponseDTO, String> getDocumentsWithMetadata(String transactionId)
			throws ResidentServiceCheckedException {
		List<ObjectDto> allObjects = objectStoreHelper.getAllObjects(transactionId);
		return allObjects.stream()
				.collect(Collectors.toMap(object -> this.fetchDocumentMetadata(transactionId, object.getObjectName()),
						object -> objectStoreHelper.getObject(transactionId + "/" + object.getObjectName())));
	}

	/**
	 * It fetches the metadata of a document from the object store
	 * 
	 * @param transactionId The transaction id of the transaction that the document
	 *                      belongs to.
	 * @param objectName    The name of the file that you want to download.
	 * @return A DocumentResponseDTO object.
	 */
	private DocumentResponseDTO fetchDocumentMetadata(String transactionId, String objectName) {
		Map<String, Object> metadata = objectStoreHelper.getMetadata(transactionId + "/" + objectName);
		return new DocumentResponseDTO(transactionId, (String) metadata.get("docid"), (String) metadata.get("docname"),
				(String) metadata.get("doccatcode"), (String) metadata.get("doctypcode"),
				StringUtils.split((String) metadata.get("docname"), "\\.")[1]);
	}

	/**
	 * It Deletes the document metadata from the object store
	 * @param transactionId
	 * @param documentId
	 * @return ResponseDTO
	 * @throws ResidentServiceCheckedException
	 */
	@Override
	public ResponseDTO deleteDocument(String transactionId, String documentId) throws ResidentServiceCheckedException {
		boolean status = objectStoreHelper.deleteObject(transactionId + "/" + documentId);
		ResponseDTO response = new ResponseDTO();
		if(status) {
			response.setStatus(SUCCESS);
			response.setMessage(DOCUMENT_DELETION_SUCCESS_MESSAGE);
		} else {
			response.setStatus(FAILURE);
			response.setMessage(DOCUMENT_DELETION_FAILURE_MESSAGE);
		}
		return response;
	}

}