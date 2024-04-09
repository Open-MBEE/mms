package org.openmbee.mms.twc.security;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.openmbee.mms.twc.config.TwcConfig;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
public class TwcAuthenticationFilterTest {

	@Mock
	TwcConfig twcConfig;

	@Mock
	TwcUserDetailsService userDetailsService;

	@Mock
	TwcAuthenticationProvider twcAuthProvider;

	@InjectMocks
	TwcAuthenticationFilter filter = new TwcAuthenticationFilter();

	@Test
	public void testNoHeadersProvided() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        FilterChain chain = mock(FilterChain.class);

        try {
            SecurityContextHolder.getContext().setAuthentication(null);
            filter.doFilterInternal(req, null, chain);
            assertNull(SecurityContextHolder.getContext().getAuthentication());

        } catch (ServletException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            verify(chain, times(1)).doFilter(any(), any());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ServletException e) {
            e.printStackTrace();
        }
    }

	@Test
	public void testNotNullAuthenticationProvider() {

		HttpServletRequest req = mock(HttpServletRequest.class);
		when(req.getHeader(anyString())).thenReturn("twc");
		FilterChain chain = mock(FilterChain.class);
		when(twcConfig.getAuthNProvider(anyString())).thenReturn(twcAuthProvider);

		try {
			SecurityContextHolder.getContext().setAuthentication(null);
			filter.doFilterInternal(req, null, chain);
			assertNotNull(twcConfig.getAuthNProvider("twc"));

		} catch (ServletException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			verify(chain, times(1)).doFilter(any(), any());
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ServletException e) {
			e.printStackTrace();
		}

	}

	@Test
	public void testNullAuthenticationProvider() {
		HttpServletRequest req = mock(HttpServletRequest.class);
		FilterChain chain = mock(FilterChain.class);
		when(twcConfig.getAuthNProvider(anyString())).thenReturn(null);

		try {
			filter.doFilterInternal(req, null, chain);
			assertNull(twcConfig.getAuthNProvider("twc"));

		} catch (ServletException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			verify(chain, times(1)).doFilter(any(), any());
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ServletException e) {
			e.printStackTrace();
		}

	}

	@Test
	public void testFailedUserAuthentication() {
		HttpServletRequest req = mock(HttpServletRequest.class);
		when(req.getHeader(anyString())).thenReturn("twc");
		FilterChain chain = mock(FilterChain.class);
		when(twcConfig.getAuthNProvider(anyString())).thenReturn(twcAuthProvider);
		when(twcAuthProvider.getAuthentication(anyString())).thenReturn(null);

		try {
			filter.doFilterInternal(req, null, chain);
			assertNull(SecurityContextHolder.getContext().getAuthentication());

		} catch (ServletException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			verify(chain, times(1)).doFilter(any(), any());
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ServletException e) {
			e.printStackTrace();
		}

	}

	@Test
	public void testPassedUserAuthentication() {
		HttpServletRequest req = mock(HttpServletRequest.class);
		when(req.getHeader(anyString())).thenReturn("twc");
		FilterChain chain = mock(FilterChain.class);
		when(twcConfig.getAuthNProvider(anyString())).thenReturn(twcAuthProvider);
		when(twcAuthProvider.getAuthentication(anyString())).thenReturn("twcUser");
		when(userDetailsService.loadUserByUsername("twcUser")).thenReturn(mock(TwcUserDetails.class));

		try {

			filter.doFilterInternal(req, null, chain);
			assertNotNull(SecurityContextHolder.getContext().getAuthentication());

		} catch (ServletException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			verify(chain, times(1)).doFilter(any(), any());
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ServletException e) {
			e.printStackTrace();
		}

	}

	@Test
	public void testException() {
		HttpServletRequest req = mock(HttpServletRequest.class);
		FilterChain chain = mock(FilterChain.class);
		when(req.getHeader(anyString())).thenAnswer((invocation) -> {
			throw new RuntimeException("Throw Exception");
		});
		try {
			filter.doFilterInternal(req, null, chain);
			assertNull(SecurityContextHolder.getContext().getAuthentication());

		} catch (ServletException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			verify(chain, times(1)).doFilter(any(), any());
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ServletException e) {
			e.printStackTrace();
		}

	}

}