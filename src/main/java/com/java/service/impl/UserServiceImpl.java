package com.java.service.impl;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.transaction.Transactional;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.social.facebook.api.Facebook;
import org.springframework.social.facebook.api.impl.FacebookTemplate;
import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.java.contants.SecurityConstants;
import com.java.dto.ChangePasswordDto;
import com.java.dto.TokenDto;
import com.java.dto.UserDto;
import com.java.entity.User;
import com.java.model.RegisterByAdmin;
import com.java.repository.RoleRepository;
import com.java.repository.UserRepository;
import com.java.service.FileService;
import com.java.service.UserService;

import net.bytebuddy.utility.RandomString;


@Service
@Scope("prototype")
public class UserServiceImpl implements UserService{
	
	private AuthenticationManager authenticationManager;
	private UserRepository userRepo;
	private RoleRepository roleRepo;
	private FileService fileService;
	private Clock cl = Clock.systemDefaultZone();
	
	public UserServiceImpl(AuthenticationManager authenticationManager,UserRepository userRepo, RoleRepository roleRepo, FileService fileService) {
		this.authenticationManager = authenticationManager;
		this.userRepo = userRepo;
		this.roleRepo = roleRepo;
		this.fileService = fileService;
	
	}

	@Override
	public List<UserDto> findAll() {
		List<User> list = userRepo.findAll();
		List<UserDto> dtos = new ArrayList<UserDto>();
		
		for(User entity : list) {
			UserDto dto = new UserDto(entity.getId(), entity.getName(),
					entity.getEmail(),"***", entity.getCreated_at(),
					entity.getLogin_token(), entity.getType(), entity.getAddress(),
					entity.getIs_email_verfied(), entity.getMobile(), entity.getImage_url(), entity.getRole_id(), entity.getDeliveryAddressId());
			dto.setNameRole(entity.getRole().getDescription()); 
			dto.setGender(entity.getGender());
			dto.setFileId(entity.getFileId());
			dtos.add(dto);
		}
		
		return dtos;
	}

	@Override
	public UserDto findById(Long id) {
		if(!userRepo.existsById(id))
			return null;
		User entity = userRepo.findById(id).get();
		UserDto dto = new UserDto(entity.getId(), entity.getName(),
				entity.getEmail(),"***", entity.getCreated_at(),
				entity.getLogin_token(), entity.getType(), entity.getAddress(),
				entity.getIs_email_verfied(), entity.getMobile(), entity.getImage_url(), entity.getRole_id(), entity.getDeliveryAddressId());
		dto.setNameRole(entity.getRole().getDescription());
		dto.setGender(entity.getGender());
		dto.setFileId(entity.getFileId());
		return dto;
	}
	
	@Transactional
	@Modifying
	@Override
	public User saveReturn(UserDto dto) throws Exception {
		if (userRepo.checkEmail(dto.getEmail()) != null)
			throw new Exception("Email already in use");
		User entity = new User();
		String hashedPass = BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt());
		entity.setLogin_token(dto.getLogin_token());
		entity.setEmail(dto.getEmail());
		entity.setMobile(dto.getMobile());
		entity.setName(dto.getName());
		entity.setPassword(hashedPass);
		entity.setCreated_at(Instant.now(cl));
		if(dto.getEmail().equalsIgnoreCase("yesthanhnhan16@gmail.com"))
			entity.setRole_id(1);
		else
			entity.setRole_id(2);
		entity.setGender(dto.getGender());
		//entity.setImage_url(ImageConstants.URL_IMAGE_AVATAR+"userDefault.png");
		entity.setImage_url("userDefault.png");
		entity.setFileId(1);
		if(userRepo.save(entity) == null)
			throw new Exception("Unable to register an account, please contact the administrator for assistance.");
		return entity;
	}
	
	@Override
	public UserDto findByEmail(String email) throws Exception {
		User entity = userRepo.checkEmail(email);
		if (entity == null)
			throw new Exception("User not found");
		if (!entity.getIs_email_verfied())
			throw new Exception("Account has not been activated!");
		UserDto dto = new UserDto(entity.getId(), entity.getName(),
				entity.getEmail(),"***", entity.getCreated_at(),
				entity.getLogin_token(), entity.getType(), entity.getAddress(),
				entity.getIs_email_verfied(), entity.getMobile(), entity.getImage_url(), entity.getRole_id(), entity.getDeliveryAddressId());
		dto.setNameRole(roleRepo.findById(entity.getRole_id()).get().getDescription());
		dto.setGender(entity.getGender());
		dto.setFileId(entity.getFileId());
		return dto;
	}

	@Override
	public int editMyAccout(UserDto dto) throws Exception {
		if(!userRepo.existsById(dto.getId()))
			throw new Exception("User not found");
		User entityTemp = userRepo.checkEmail(dto.getEmail());
		
		// n???u entityTemp c?? t???n t???i nh??ng id l???i kh??c v???i id c???a ng?????i update th?? => ???? c?? ng?????i ????ng k?? email n??y r
		if(entityTemp != null && entityTemp.getId() != dto.getId()) {
			throw new Exception("This email already exists");
		}
		User entity = userRepo.findById(dto.getId()).get();
		entity.setEmail(dto.getEmail());
		entity.setMobile(dto.getMobile());
		entity.setName(dto.getName());
		entity.setAddress(dto.getAddress());
		entity.setDeliveryAddressId(dto.getDeliveryAddressId());
		entity.setImage_url(dto.getImage_url());
		entity.setGender(dto.getGender());
		entity.setFileId(dto.getFileId());
		if(userRepo.save(entity) == null)
			throw new Exception("Unable to update");
		return 0;
	}

	@Override
	public int updateActive(UserDto dto) throws Exception {
		userRepo.updateActive(dto.getId(), dto.getIs_email_verfied());
		return 0;
	}

	@Override
	public List<UserDto> search(String key) {
		List<UserDto> dtos = userRepo.search(key);
		return dtos;
	}

	@Transactional
	@Modifying
	@Override
	public Integer delete(Long id, Long idFile) throws Exception {
		this.userRepo.deleteById(id);
		this.fileService.deleteFileDB(idFile);
		return 0;
	}
	@Override
	public List<UserDto> deleteDtos(Long id, Long fileId) throws Exception {
		this.delete(id, fileId);
		List<UserDto> dtos= userRepo.FindAllByDelete();
		return dtos;
	}

	@Override
	public int editMyAccoutByAdmin(UserDto dto) throws Exception {
		if(!userRepo.existsById(dto.getId()))
			throw new Exception("User not found");
		User entityTemp = userRepo.checkEmail(dto.getEmail());
		
		// n???u entityTemp c?? t???n t???i nh??ng id l???i kh??c v???i id c???a ng?????i update th?? => ???? c?? ng?????i ????ng k?? email n??y r
		if(entityTemp != null && entityTemp.getId() != dto.getId()) {
			throw new Exception("This email already exists");
		}
		User entity = userRepo.findById(dto.getId()).get();
		entity.setEmail(dto.getEmail());
		entity.setMobile(dto.getMobile());
		entity.setName(dto.getName());
		entity.setAddress(dto.getAddress());
		entity.setImage_url(dto.getImage_url());
		entity.setGender(dto.getGender());
		entity.setRole_id(dto.getRole_id());
		entity.setIs_email_verfied(dto.getIs_email_verfied());
		entity.setFileId(dto.getFileId());
		if(userRepo.save(entity) == null)
			throw new Exception("Unable to update");
		return 0;
	}

	@Override
	public void addByAdmin(RegisterByAdmin admin) throws Exception {
		if (userRepo.checkEmail(admin.getEmail()) != null)
			throw new Exception("Email already exists");
		
		User entity = new User();
		String hashedPass = BCrypt.hashpw(admin.getPassword(), BCrypt.gensalt());
		entity.setEmail(admin.getEmail());
		entity.setMobile(admin.getMobile());
		entity.setName(admin.getName());
		entity.setPassword(hashedPass);
		entity.setCreated_at(Instant.now(cl));
		entity.setRole_id(admin.getRole_id());
		entity.setGender(admin.getGender());
		entity.setIs_email_verfied(admin.isIs_email_verfied());
		//entity.setImage_url(ImageConstants.URL_IMAGE_AVATAR+"userDefault.png");
		entity.setImage_url("userDefault.png");
		entity.setFileId(1);
		if(userRepo.save(entity) == null) 
			throw new Exception("Can't add");
		
	}

	@Override
	public Integer save(UserDto dto) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void checkCode(String code, String email) throws Exception{
		//Ki???m tra xem c?? t???n t???i email n??y kh??ng
		User entity = userRepo.checkEmail(email);
		if(entity == null) {
			throw new Exception("User not found.");
		}
		//sau ???? ki???m tra xem t??i kho???n ???? ???????c k??ch ho???t ch??a n???u ch??a th?? m???i ti???p t???c so s??nh v???i code
		
		if(entity.getIs_email_verfied() == true)
			throw new Exception("Account has been activated.");
		
		//so s??nh code
		if(!entity.getLogin_token().equals(code))
			throw new Exception("Verification code is incorrect.");
		
		//C???p k??ch ho???t t??i kho???n sau khi code kh???p
		entity.setIs_email_verfied(true);
		userRepo.save(entity);
	}

	public void signUpGoogle(Payload payload) throws IOException {
		if (userRepo.checkEmail(payload.getEmail()) != null)
			throw new IOException("Email already in use");
		User entity = new User();
		String hashedPass = BCrypt.hashpw(SecurityConstants.passSecret, BCrypt.gensalt());
		entity.setLogin_token(RandomString.make(64));
		entity.setIs_email_verfied(payload.getEmailVerified());
		entity.setEmail(payload.getEmail());
		entity.setName((String)payload.get("name"));
		entity.setPassword(hashedPass);
		entity.setCreated_at(Instant.now(cl));
		if(payload.getEmail().equalsIgnoreCase("yesthanhnhan16@gmail.com"))
			entity.setRole_id(1);
		else
			entity.setRole_id(2);
		entity.setGender("Prefer not to say");
		entity.setImage_url("userDefault.png");
		entity.setFileId(1);
		if(userRepo.save(entity) == null)
			throw new IOException("Unable to register an account, please contact the administrator for assistance.");
	}

	public void signUpFacebook(org.springframework.social.facebook.api.User user) throws IOException {
		if (userRepo.checkEmail(user.getEmail()) != null)
			throw new IOException("Email already in use");
		User entity = new User();
		String hashedPass = BCrypt.hashpw(SecurityConstants.passSecret, BCrypt.gensalt());
		entity.setLogin_token(RandomString.make(64));
		entity.setEmail(user.getEmail());
		entity.setName(user.getName());
		entity.setPassword(hashedPass);
		entity.setCreated_at(Instant.now(cl));
		if(user.getEmail().equalsIgnoreCase("yesthanhnhan16@gmail.com"))
			entity.setRole_id(1);
		else
			entity.setRole_id(2);
		entity.setGender("Prefer not to say");
		entity.setImage_url("userDefault.png");
		entity.setFileId(1);
		if(userRepo.save(entity) == null)
			throw new IOException("Unable to register an account, please contact the administrator for assistance.");
	}
	
	

	@Override
	public void signUpWithSocialMediaGoogle(TokenDto dto) throws IOException {
		Payload payload = this.signInGoogle(dto);
		this.signUpGoogle(payload);
		
	}

	@Override
	public void signUpWithSocialMediaFacebook(TokenDto dto) throws IOException{
		org.springframework.social.facebook.api.User user = this.signInFacebook(dto);
		this.signUpFacebook(user);
	}

	@Override
	public Payload signInGoogle(TokenDto dto) throws IOException {
		final NetHttpTransport httpTransport = new NetHttpTransport();
		final JacksonFactory factory = JacksonFactory.getDefaultInstance();
		GoogleIdTokenVerifier.Builder builder = 
				new GoogleIdTokenVerifier.Builder(httpTransport, factory)
				.setAudience(Collections.singletonList(SecurityConstants.googleClientId));
		final GoogleIdToken googleIdToken = GoogleIdToken.parse(builder.getJsonFactory(), dto.getValue());
		final GoogleIdToken.Payload payload = googleIdToken.getPayload();
		return payload;
	}

	@Override
	public org.springframework.social.facebook.api.User signInFacebook(TokenDto dto) throws IOException {
		Facebook facebook = new FacebookTemplate(dto.getValue());
		final String []fields = {"email", "name"};
		org.springframework.social.facebook.api.User user = facebook.fetchObject("me", org.springframework.social.facebook.api.User.class, fields);
		
		return user;
	}

	@Override
	public void changePassword(ChangePasswordDto dto) throws Exception {
		try {
			Authentication authentication = new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getOldPassword());
			authenticationManager.authenticate(authentication);
			User entity = userRepo.checkEmail(dto.getEmail());
			entity.setPassword(BCrypt.hashpw(dto.getConfirmPassword(), BCrypt.gensalt()));
			userRepo.save(entity);
		} catch (Exception e) {
			System.out.println("sai");
			throw new Exception("Current password is incorrect!");
		}
	}

	@Override
	public Integer delete(Long id) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
}
