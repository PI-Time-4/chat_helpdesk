package br.gov.sp.fatec.springbootapp.service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.stereotype.Service;

import br.gov.sp.fatec.springbootapp.entity.Autorizacao;
import br.gov.sp.fatec.springbootapp.entity.Usuario;

import br.gov.sp.fatec.springbootapp.exception.RegistroNaoEncontradoException;

import br.gov.sp.fatec.springbootapp.repository.AutorizacaoRepository;
import br.gov.sp.fatec.springbootapp.repository.UsuarioRepository;

@Service("SegurancaService")
public class SegurancaServiceImpl implements SegurancaService {

    @Autowired
    private AutorizacaoRepository autRepo;
    @Autowired
    private UsuarioRepository usuarioRepo;
    @Autowired
    private PasswordEncoder passEncoder;

    @Transactional
    public Usuario criarUsuario(String nome, String senha, String autorizacao) {

        Autorizacao aut = autRepo.findByNome(autorizacao);
        if (aut == null) {
            aut = new Autorizacao();
            aut.setNome(autorizacao);
            autRepo.save(aut);
        }
        Usuario usuario = new Usuario();
        usuario.setNome(nome);
        usuario.setSenha(passEncoder.encode(senha));
        usuario.setAutorizacoes(new HashSet<Autorizacao>());
        usuario.getAutorizacoes().add(aut);
        usuarioRepo.save(usuario);
        return usuario;
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')") // está autenticado, param entrada
    public List<Usuario> buscarTodosUsuarios() {
        return usuarioRepo.findAll();

    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'USUARIO')")
    public Usuario buscarUsuarioPorId(Long id) {
        Optional<Usuario> usuarioOP = usuarioRepo.findById(id);
        if (usuarioOP.isPresent()) {
            return usuarioOP.get();
        } else {
            throw new RegistroNaoEncontradoException("usuario nao encontrado");
        }

    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public Usuario buscarUsuarioPorNome(String nome) {
        Usuario usuario = usuarioRepo.findByNome(nome);
        if (usuario != null) {
            return usuario;
        } else {
            throw new RegistroNaoEncontradoException("usuario nao encontrado");
        }

    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public Autorizacao buscarAutorizacaoPorNome(String nome) {
        Autorizacao autorizacao = autRepo.findByNome(nome);
        if (autorizacao != null) {
            return autorizacao;
        }
        throw new RegistroNaoEncontradoException("autorizacao não encontrada");
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepo.findByNome(username);
        if (usuario == null) {
            throw new UsernameNotFoundException("Usuário" + username + "não encontrado");
        }
        return User.builder().username(username).password(usuario.getSenha())
        .authorities(usuario.getAutorizacoes().stream()
            .map(Autorizacao::getNome).collect(Collectors.toList())
            .toArray(new String[usuario.getAutorizacoes().size()]))
        .build();
    }

    @Override
    public Usuario buscarUsuarioPorNomeESenha(String nome, String senha) {
        Usuario usuario = usuarioRepo.buscaUsuaioPorNomeESenha(nome, senha);
        
        return usuario;
    }

    @Override
    @Transactional
    public Usuario alterarSenha(String senha, Long id) {
        Optional<Usuario> usuarioOp = usuarioRepo.findById(id);

        if(usuarioOp.isPresent()){
            Usuario usuario = usuarioOp.get();

            usuario.setSenha(senha);
            usuarioRepo.save(usuario);
            return usuario;
        
        }
        throw new RegistroNaoEncontradoException("Usuario não encontrado");
    }
}