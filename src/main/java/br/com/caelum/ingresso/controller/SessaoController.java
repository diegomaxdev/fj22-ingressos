package br.com.caelum.ingresso.controller;

import java.util.List;
import java.util.Optional;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import br.com.caelum.ingresso.dao.FilmeDao;
import br.com.caelum.ingresso.dao.SalaDao;
import br.com.caelum.ingresso.dao.SessaoDao;
import br.com.caelum.ingresso.model.Carrinho;
import br.com.caelum.ingresso.model.ImagemCapa;
import br.com.caelum.ingresso.model.Sessao;
import br.com.caelum.ingresso.model.SessaoForm;
import br.com.caelum.ingresso.model.TipoDeIngresso;
import br.com.caelum.ingresso.rest.OmdbClient;
import br.com.caelum.ingresso.validacao.GerenciadorDeSessao;

@Controller
public class SessaoController 
{
	//passando a responsabilidade para o Spring
	@Autowired
	private SalaDao salaDao;
	
	@Autowired
	private FilmeDao filmeDao;
	
	@Autowired
	private SessaoDao sessaoDao;
	
    @Autowired
    private OmdbClient client;
    
    @Autowired
    private Carrinho carrinho;
	
	@GetMapping("/admin/sessao")
	//identificador de parametros que vão ser passados pelo navegador
	public ModelAndView form(@RequestParam("salaId") Integer salaId, SessaoForm form) 
	{
		//passando meu jsp para onde eu vou retornar
		ModelAndView mv = new ModelAndView("sessao/sessao");
		//temos que passar exatamente o que está sendo chamado em meu jsp - deve ser igual a referencia
		mv.addObject("sala", salaDao.findOne(salaId));
		//populando um Model and View para enviálo carregado
		mv.addObject("filmes", filmeDao.findAll());
		//Passando uma formSessao recebido via parametro
		mv.addObject("form", form);
		
		return mv;
	}
	
	
	@GetMapping("/sessao/{id}/lugares")
	public ModelAndView lugaresNaSessao(@PathVariable("id") Integer sessaoId) 
	{
		//passando meu jsp para onde eu vou retornar
		ModelAndView mv = new ModelAndView("sessao/lugares");
		//temos que passar exatamente o que está sendo chamado em meu jsp - deve ser igual a referencia
		Sessao sessao = sessaoDao.findOne(sessaoId);
		Optional<ImagemCapa> imagemCapa = client.request(sessao.getFilme(), ImagemCapa.class);
		//populando um Model and View para enviálo carregado
		mv.addObject("sessao", sessao);
		mv.addObject("carrinho", carrinho);
		mv.addObject("imagemCapa", imagemCapa.orElse(new ImagemCapa()));
		mv.addObject("tiposDeIngressos", TipoDeIngresso.values());
		return mv;
	}
	
	/*
	 
	 	@GetMapping("/sessao/{id}/lugares")
	public ModelAndView form(@RequestParam("id") Integer sessaoId) 
	{
		//passando meu jsp para onde eu vou retornar
		ModelAndView mv = new ModelAndView("sessao/lugares");
		//temos que passar exatamente o que está sendo chamado em meu jsp - deve ser igual a referencia
		Sessao sessao = sessaoDao.findOne(sessaoId);
		//populando um Model and View para enviálo carregado
		mv.addObject("sessao", sessao);
		return mv;
	}
	 
	 * */
	
	@PostMapping(value = "/admin/sessao")
	@Transactional
	public ModelAndView salva(@Valid SessaoForm form, BindingResult result) 
	{
		if(result.hasErrors()) 
		{
			return form(form.getSalaId(),form);
		}
		
		Sessao sessao = form.toSessao(salaDao, filmeDao);
		
		List<Sessao> sessoesDaSala = sessaoDao.buscaSessoesDaSala(sessao.getSala());
		GerenciadorDeSessao gerenciador = new GerenciadorDeSessao(sessoesDaSala);
		
		if(gerenciador.cabe(sessao)) 
		{
			sessaoDao.save(sessao);
			return new ModelAndView("redirect:/admin/sala/" + form.getSalaId() + "/sessoes");
		}
		
		return form(form.getSalaId(),form);
	}
}
